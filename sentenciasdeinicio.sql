INSERT INTO public.user_roles
(user_id, role_id)
VALUES(1, 1);
INSERT INTO public.user_roles
(user_id, role_id)
VALUES(2, 1);

INSERT INTO public.users
(id, active, apellido, email, intentos, nombre, "password", secret, two_factor_enabled, username)
VALUES(1, true, 'romero', 'tecnicohome@hotmail.es', NULL, 'juanma', '$2a$10$7q9Ud8bV0h2BgI1U5OG9eewPzH90v1O7/9Q7ZkAYBfXn7Ft3m7YPm', '$2a$10$7q9Ud8bV0h2BgI1U5OG9eewPzH90v1O7/9Q7ZkAYBfXn7Ft3m7YPm', true, 'admin');
INSERT INTO public.users
(id, active, apellido, email, intentos, nombre, "password", secret, two_factor_enabled, username)
VALUES(2, true, 'Peláez', 'jmrptcm@hotmail.com', 0, 'Juan Manuel Romero', '$2a$10$KZ9KckuecZAONW4kKson1.EdezUGi.bwGbhYbq6HTRShH4N1gyuZK', '914993', false, 'juanma40');