"""Loopback-only development email capture; no outgoing mail or dependencies."""
import email
import email.policy
import html
import re
import socketserver
import threading
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

MAIL = Path(__file__).resolve().parents[1] / '.local' / 'mail'
MAIL.mkdir(parents=True, exist_ok=True)

class SmtpHandler(socketserver.StreamRequestHandler):
    def handle(self):
        self.connection.settimeout(30)
        self.wfile.write(b'220 Schoolhouse local inbox\r\n')
        while True:
            line = self.rfile.readline(65536)
            if not line:
                return
            verb = line.split(b' ', 1)[0].strip().upper()
            if verb in (b'EHLO', b'HELO'):
                self.wfile.write(b'250 localhost\r\n')
            elif verb in (b'MAIL', b'RCPT', b'RSET', b'NOOP'):
                self.wfile.write(b'250 OK\r\n')
            elif verb == b'DATA':
                self.wfile.write(b'354 End with a single dot\r\n')
                content = bytearray()
                while True:
                    part = self.rfile.readline(65536)
                    if not part or part == b'.\r\n':
                        break
                    content.extend(part[1:] if part.startswith(b'..') else part)
                    if len(content) > 1048576:
                        self.wfile.write(b'552 Message too large\r\n')
                        return
                target = MAIL / (str(uuid.uuid4()) + '.eml')
                target.write_bytes(content)
                target.chmod(0o600)
                self.wfile.write(b'250 Captured locally\r\n')
            elif verb == b'QUIT':
                self.wfile.write(b'221 Goodbye\r\n')
                return
            else:
                self.wfile.write(b'502 Command not supported\r\n')

class InboxHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != '/':
            self.send_error(404)
            return
        cards = []
        for path in sorted(MAIL.glob('*.eml'), key=lambda p: p.stat().st_mtime, reverse=True):
            message = email.message_from_bytes(path.read_bytes(), policy=email.policy.default)
            body = message.get_body(preferencelist=('plain',)) if message.is_multipart() else message
            text = body.get_content() if body else ''
            rendered = html.escape(str(text))
            rendered = re.sub(r'(http://localhost:8080/[^\s<>"\']+)', r'<a href="\1">\1</a>', rendered)
            cards.append('<article><h2>' + html.escape(str(message.get('Subject', 'Email'))) +
                         '</h2><p>To: ' + html.escape(str(message.get('To', ''))) + '</p><pre>' + rendered + '</pre></article>')
        page = ('<!doctype html><meta charset="utf-8"><title>Schoolhouse local inbox</title>'
                '<style>body{font:15px system-ui;max-width:900px;margin:40px auto;padding:20px;background:#f6f7f4;color:#253a32}'
                'article{background:white;border:1px solid #ddd;padding:20px;margin:20px 0;border-radius:8px}'
                'pre{white-space:pre-wrap;overflow-wrap:anywhere}a{color:#225c44}</style>'
                '<h1>Schoolhouse local inbox</h1><p>Development emails are captured here; nothing is sent externally.</p>'
                '<p><a href="/">Refresh inbox</a> · <a href="http://localhost:8080">Open school application</a></p>' +
                (''.join(cards) or '<p>No emails yet.</p>')).encode()
        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Content-Length', str(len(page)))
        self.send_header('Cache-Control', 'no-store')
        self.end_headers()
        self.wfile.write(page)
    def log_message(self, *args):
        pass

class SmtpServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

if __name__ == '__main__':
    smtp = SmtpServer(('127.0.0.1', 1025), SmtpHandler)
    threading.Thread(target=smtp.serve_forever, daemon=True).start()
    ThreadingHTTPServer(('127.0.0.1', 8025), InboxHandler).serve_forever()
