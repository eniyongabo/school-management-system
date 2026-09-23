"""Start the local MySQL/API/React/email stack without Docker."""
import base64
import json
import os
from pathlib import Path
import secrets
import shutil
import socket
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.local'
STATE.mkdir(mode=0o700, exist_ok=True)
STATE.chmod(0o700)
CREDENTIALS = STATE / 'credentials.json'
if not CREDENTIALS.exists():
    credentials = {
        'mysql_root_password': secrets.token_hex(24),
        'db_password': secrets.token_hex(24),
        'jwt_secret': base64.b64encode(secrets.token_bytes(32)).decode(),
        'admin_email': 'admin@school.local',
        'admin_password': secrets.token_urlsafe(18),
    }
    CREDENTIALS.write_text(json.dumps(credentials, indent=2) + '\n')
    CREDENTIALS.chmod(0o600)
credentials = json.loads(CREDENTIALS.read_text())

for executable in ['java', 'mysql', 'mysqld', 'npm']:
    if not shutil.which(executable):
        raise SystemExit('Missing required executable: ' + executable)

pids_file = STATE / 'processes.json'
pids = json.loads(pids_file.read_text()) if pids_file.exists() else {}

def listening(port):
    with socket.socket() as connection:
        connection.settimeout(.5)
        return connection.connect_ex(('127.0.0.1', port)) == 0

def wait_port(port, process, timeout=90):
    until = time.time() + timeout
    while time.time() < until:
        if listening(port):
            return
        if process.poll() is not None:
            raise RuntimeError(f'Service on port {port} exited. See logs in {STATE}')
        time.sleep(.5)
    raise RuntimeError(f'Timeout waiting for port {port}. See logs in {STATE}')

def launch(name, command, port, env=None, cwd=ROOT):
    if listening(port):
        known = pids.get(name)
        command_line = subprocess.run(['ps', '-p', str(known or 0), '-o', 'command='], capture_output=True, text=True).stdout
        if not known or not command_line:
            raise RuntimeError(f'Port {port} is already occupied. No existing service was stopped.')
        return
    with (STATE / (name + '.log')).open('ab') as log:
        process = subprocess.Popen(command, cwd=cwd, env=env, stdin=subprocess.DEVNULL,
                                   stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    pids[name] = process.pid
    pids_file.write_text(json.dumps(pids, indent=2))
    wait_port(port, process)

socket_path = STATE / 'mysql.sock'
data_dir = STATE / 'mysql-data'
if not (data_dir / 'mysql').exists():
    print('Initializing dedicated local MySQL storage...', flush=True)
    subprocess.run(['mysqld', '--no-defaults', '--initialize-insecure', '--datadir=' + str(data_dir),
                    '--log-error=' + str(STATE / 'mysql-init.log')], check=True)
launch('mysql', ['mysqld', '--no-defaults', '--datadir=' + str(data_dir), '--port=13317',
    '--bind-address=127.0.0.1', '--mysqlx=0', '--socket=' + str(socket_path),
    '--pid-file=' + str(STATE / 'mysql.pid'), '--log-error=' + str(STATE / 'mysql-server.log')], 13317)

client_config = STATE / 'mysql-client.cnf'
client_config.write_text('[client]\nuser=root\npassword=' + credentials['mysql_root_password'] + '\nsocket=' + str(socket_path) + '\n')
client_config.chmod(0o600)
client = ['mysql', '--defaults-file=' + str(client_config)]
probe = subprocess.run(client + ['-e', 'SELECT 1'], capture_output=True)
if probe.returncode:
    # Only the dedicated fresh server is eligible for initial empty-root provisioning.
    client = ['mysql', '--no-defaults', '--socket=' + str(socket_path), '-uroot']
sql = "CREATE DATABASE IF NOT EXISTS school_management;\n"
sql += "CREATE USER IF NOT EXISTS 'school_app'@'localhost' IDENTIFIED BY '" + credentials['db_password'] + "';\n"
sql += "GRANT ALL PRIVILEGES ON school_management.* TO 'school_app'@'localhost';\n"
sql += "ALTER USER 'root'@'localhost' IDENTIFIED BY '" + credentials['mysql_root_password'] + "';\n"
result = subprocess.run(client, input=sql, text=True, capture_output=True)
if result.returncode:
    raise RuntimeError('Local MySQL provisioning failed; no credentials were printed.')

launch('mail', [sys.executable, str(ROOT / 'scripts/local-mail.py')], 1025)
api_env = os.environ.copy()
api_env.update(DB_URL='jdbc:mysql://127.0.0.1:13317/school_management?connectionTimeZone=UTC&allowPublicKeyRetrieval=true&useSSL=false',
               DB_USERNAME='school_app', DB_PASSWORD=credentials['db_password'], JWT_SECRET=credentials['jwt_secret'],
               ADMIN_EMAIL=credentials['admin_email'], ADMIN_PASSWORD=credentials['admin_password'],
               SMTP_HOST='127.0.0.1', SMTP_PORT='1025', SMTP_USERNAME='', SMTP_PASSWORD='', SMTP_AUTH='false', SMTP_TLS='false',
               FRONTEND_ORIGIN='http://localhost:8080', PORT='8081', SERVER_ADDRESS='127.0.0.1')
jar = ROOT / 'backend/target/school-management-api-0.1.0-SNAPSHOT.jar'
if not jar.exists():
    raise RuntimeError('Build the backend with Maven before starting.')
launch('api', ['java', '-jar', str(jar)], 8081, api_env)
web_env = os.environ.copy()
web_env['VITE_PROXY_TARGET'] = 'http://127.0.0.1:8081'
web_env.pop('VITE_API_URL', None)
launch('web', ['npm', 'run', 'dev', '--', '--port', '8080', '--strictPort'], 8080, web_env, ROOT / 'frontend')

for attempt in range(60):
    try:
        with urllib.request.urlopen('http://127.0.0.1:8080/actuator/health', timeout=3) as response:
            if json.load(response)['status'] == 'UP':
                break
    except Exception:
        time.sleep(1)
else:
    raise RuntimeError('Health check did not reach UP. Inspect .local logs.')
request = urllib.request.Request('http://127.0.0.1:8080/api/auth/login',
    data=json.dumps({'email': credentials['admin_email'], 'password': credentials['admin_password']}).encode(),
    headers={'Content-Type': 'application/json', 'Origin': 'http://localhost:8080'})
with urllib.request.urlopen(request, timeout=10) as response:
    session = json.load(response)
with urllib.request.urlopen(urllib.request.Request('http://127.0.0.1:8080/api/dashboard',
    headers={'Authorization': 'Bearer ' + session['accessToken']}), timeout=10) as response:
    json.load(response)
print('Application: http://localhost:8080')
print('Local email inbox: http://localhost:8025')
print('Administrator email: ' + credentials['admin_email'])
print('Administrator password: ' + credentials['admin_password'])
print('Verified: database/email health, administrator login, and authenticated dashboard.')
