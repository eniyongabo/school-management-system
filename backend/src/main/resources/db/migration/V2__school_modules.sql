CREATE TABLE schedules (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, class_subject_id BIGINT NOT NULL, weekday INT NOT NULL,
 start_time TIME NOT NULL,end_time TIME NOT NULL,room VARCHAR(80) NOT NULL,
 FOREIGN KEY(class_subject_id) REFERENCES class_subjects(id), CHECK(weekday BETWEEN 1 AND 7), CHECK(end_time>start_time)
);
CREATE TABLE assignments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,class_subject_id BIGINT NOT NULL,title VARCHAR(200) NOT NULL,
 description VARCHAR(10000) NOT NULL,due_at TIMESTAMP NOT NULL,FOREIGN KEY(class_subject_id) REFERENCES class_subjects(id)
);
CREATE TABLE grades (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,class_subject_id BIGINT NOT NULL,term_id BIGINT NOT NULL,
 type VARCHAR(20) NOT NULL,title VARCHAR(200) NOT NULL,score DECIMAL(8,2) NOT NULL,maximum DECIMAL(8,2) NOT NULL,
 weight DECIMAL(8,2) NOT NULL,author_id BIGINT NOT NULL,updated_at TIMESTAMP NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(student_id) REFERENCES students(id),FOREIGN KEY(class_subject_id) REFERENCES class_subjects(id),FOREIGN KEY(term_id) REFERENCES terms(id),FOREIGN KEY(author_id) REFERENCES users(id),
 UNIQUE(student_id,class_subject_id,term_id,type,title), CHECK(score>=0 AND score<=maximum),CHECK(maximum>0 AND weight>0),CHECK(type IN ('ASSIGNMENT','QUIZ','EXAM','MIDTERM','FINAL'))
);
CREATE TABLE attendance (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,class_id BIGINT NOT NULL,attendance_date DATE NOT NULL,status VARCHAR(20) NOT NULL,author_id BIGINT NOT NULL,
 UNIQUE(student_id,class_id,attendance_date),FOREIGN KEY(student_id) REFERENCES students(id),FOREIGN KEY(class_id) REFERENCES classes(id),FOREIGN KEY(author_id) REFERENCES users(id),CHECK(status IN ('PRESENT','ABSENT','LATE','EXCUSED'))
);
CREATE TABLE fee_structures (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,academic_year_id BIGINT NOT NULL,class_id BIGINT,name VARCHAR(120) NOT NULL,amount DECIMAL(12,2) NOT NULL,currency VARCHAR(3) NOT NULL,
 FOREIGN KEY(academic_year_id) REFERENCES academic_years(id),FOREIGN KEY(class_id) REFERENCES classes(id),CHECK(amount>0)
);
CREATE TABLE invoices (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,invoice_number VARCHAR(50) NOT NULL UNIQUE,description VARCHAR(200) NOT NULL,
 amount DECIMAL(12,2) NOT NULL,currency VARCHAR(3) NOT NULL,due_date DATE NOT NULL,issued_at TIMESTAMP NOT NULL,version BIGINT NOT NULL DEFAULT 0,
 FOREIGN KEY(student_id) REFERENCES students(id),CHECK(amount>0)
);
CREATE TABLE payments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,invoice_id BIGINT NOT NULL,payer_id BIGINT NOT NULL,amount DECIMAL(12,2) NOT NULL,currency VARCHAR(3) NOT NULL,
 idempotency_key VARCHAR(100) NOT NULL UNIQUE,provider_reference VARCHAR(100) NOT NULL UNIQUE,status VARCHAR(20) NOT NULL,paid_at TIMESTAMP NOT NULL,
 FOREIGN KEY(invoice_id) REFERENCES invoices(id),FOREIGN KEY(payer_id) REFERENCES users(id),CHECK(amount>0),CHECK(status IN ('SUCCEEDED'))
);
CREATE TABLE receipts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,payment_id BIGINT NOT NULL UNIQUE,receipt_number VARCHAR(50) NOT NULL UNIQUE,issued_at TIMESTAMP NOT NULL,
 FOREIGN KEY(payment_id) REFERENCES payments(id)
);
CREATE TABLE announcements (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,author_id BIGINT NOT NULL,class_id BIGINT,title VARCHAR(200) NOT NULL,body VARCHAR(10000) NOT NULL,priority VARCHAR(20) NOT NULL,published_at TIMESTAMP NOT NULL,
 FOREIGN KEY(author_id) REFERENCES users(id),FOREIGN KEY(class_id) REFERENCES classes(id),CHECK(priority IN ('NORMAL','IMPORTANT','EMERGENCY'))
);
CREATE TABLE notifications (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,recipient_id BIGINT NOT NULL,announcement_id BIGINT NOT NULL,title VARCHAR(200) NOT NULL,created_at TIMESTAMP NOT NULL,read_at TIMESTAMP NULL,
 FOREIGN KEY(recipient_id) REFERENCES users(id),FOREIGN KEY(announcement_id) REFERENCES announcements(id),UNIQUE(recipient_id,announcement_id)
);
CREATE INDEX idx_notification_recipient ON notifications(recipient_id,created_at);
CREATE TABLE messages (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,sender_id BIGINT NOT NULL,recipient_id BIGINT NOT NULL,body VARCHAR(5000) NOT NULL,sent_at TIMESTAMP NOT NULL,read_at TIMESTAMP NULL,
 FOREIGN KEY(sender_id) REFERENCES users(id),FOREIGN KEY(recipient_id) REFERENCES users(id)
);
CREATE INDEX idx_message_recipient ON messages(recipient_id,sent_at);
CREATE INDEX idx_message_sender ON messages(sender_id,sent_at);
CREATE TABLE events (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(200) NOT NULL,description VARCHAR(5000),category VARCHAR(30) NOT NULL,start_at TIMESTAMP NOT NULL,end_at TIMESTAMP NOT NULL,all_day BOOLEAN NOT NULL,author_id BIGINT NOT NULL,
 FOREIGN KEY(author_id) REFERENCES users(id),CHECK(end_at>=start_at)
);
CREATE INDEX idx_event_start ON events(start_at);
CREATE TABLE documents (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,assignment_id BIGINT NOT NULL,uploader_id BIGINT NOT NULL,filename VARCHAR(255) NOT NULL,media_type VARCHAR(100) NOT NULL,content LONGBLOB NOT NULL,uploaded_at TIMESTAMP NOT NULL,
 FOREIGN KEY(assignment_id) REFERENCES assignments(id),FOREIGN KEY(uploader_id) REFERENCES users(id)
);
CREATE TABLE submissions (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,assignment_id BIGINT NOT NULL,student_id BIGINT NOT NULL,body VARCHAR(10000) NOT NULL,submitted_at TIMESTAMP NOT NULL,
 FOREIGN KEY(assignment_id) REFERENCES assignments(id),FOREIGN KEY(student_id) REFERENCES students(id),UNIQUE(assignment_id,student_id)
);
CREATE TABLE audit_logs (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,actor_id BIGINT NOT NULL,action VARCHAR(80) NOT NULL,resource_type VARCHAR(80) NOT NULL,resource_id BIGINT NOT NULL,occurred_at TIMESTAMP NOT NULL,
 FOREIGN KEY(actor_id) REFERENCES users(id)
);
CREATE INDEX idx_audit_time ON audit_logs(occurred_at);
