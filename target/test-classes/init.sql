-- 1. Создаем последовательность для ID
CREATE SEQUENCE IF NOT EXISTS users_info_id_seq;

-- 2. Создаем таблицу (упростил COLLATE для совместимости с любым образом Postgres)
CREATE TABLE IF NOT EXISTS public.users_info
(
    id integer NOT NULL DEFAULT nextval('users_info_id_seq'),
    name text NOT NULL,
    ip_address inet,
    country varchar(100),
    city varchar(100),
    comment text,
    CONSTRAINT users_info_pkey PRIMARY KEY (id)
);

-- 3. Добавляем те самые 2 записи для теста
INSERT INTO public.users_info (name, country, city, comment) 
VALUES ('InternetTelecom', 'Russia', 'St.Petersburg', 'SPB team'),
       ('Terayon', 'USA', 'Los Angeles', 'USA team');