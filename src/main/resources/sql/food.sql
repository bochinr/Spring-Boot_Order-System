use order_system;

CREATE TABLE food (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    info TEXT,
    price INT,
    level INT
);

INSERT INTO food (id, name, info, price, level) VALUES
(1, '宫保鸡丁', '经典川菜，口感麻辣鲜香', 28, 3),
(2, '麻婆豆腐', '四川传统名菜，麻辣可口', 22, 2),
(3, '清蒸鲈鱼', '清淡爽口，保持鱼的原汁原味', 35, 4),
(4, '红烧肉', '肥而不腻，入口即化', 30, 3),
(5, '酸辣汤', '开胀解腻的美味汤品', 18, 2),
(6, '糖醋排骨', '外酥里嫩，甜酸可口', 25, 3),
(7, '鱼香肉丝', '川菜代表，咸鲜开胃', 24, 3),
(8, '水煮肉片', '麻辣鲜香，肉质滑嫩', 26, 3);
