use order_system;

CREATE TABLE user (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    age INT,
    UNIQUE KEY unique_name (name)
);

-- 插入一些测试数据
INSERT INTO user (name, password, age) VALUES
('admin', 'admin123', 25),
('test', 'test123', 30); 