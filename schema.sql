--
/*drop database if exists latest;
create database latest;*/

drop table if exists public.users;
create table public.users (
	id integer not null,
	firstname varchar not null,
	lastname varchar not null,
	email varchar unique not null,
	constraint users_pk primary key (id)
);


drop table if exists public.programs;
create table public.programs(
	id integer not null,
	"name" varchar not null,
	description varchar not null,
	version integer not null,
	constraint programs_pk primary key (id)
);

drop table if exists public.courses;
create table public.courses(
	id integer not null,
	"name" varchar not null,
	description varchar,
	constraint courses_pk primary key (id)
);

drop table if exists public.contents;
create table public.contents(
	id integer not null,
	"name" varchar unique not null,
	description varchar,
	total integer,
	course_id integer,

	constraint fk_course foreign key(course_id) references courses(id),
	constraint contents_pk primary key (id)
);

drop table if exists public.course_progress;
create table public.course_progress(
	program_id integer,
	course_id integer,
	content_id integer,
	user_id integer,
	completed integer NOT NULL,
    percentage numeric(3,2) GENERATED ALWAYS AS (((completed)::numeric / total)) STORED,
    total numeric NOT null,

	constraint fk_program foreign key(program_id) references programs(id),
	constraint fk_course foreign key(course_id) references courses(id),
	constraint fk_content foreign key(content_id) references contents(id),
	constraint fk_user foreign key(user_id) references users(id),
	constraint course_progress_pk primary key (program_id, course_id, content_id, user_id)
);



INSERT INTO public.users VALUES (1, 'user1', 'lastname1', 'user1@applaudostudios.com');

INSERT INTO public.courses VALUES (1, 'Akka http / Akka Persistence', 'Course to learn frameworks for Akka HTTP and Akka persistence.');
INSERT INTO public.courses VALUES (2, 'Reactive programming paradigm', 'Reactive architecture concepts.');

INSERT INTO public.programs VALUES (1, 'scala cross-training', 'designed for Applaudo employees to level up Scala and Akka technologies.', 1);

INSERT INTO public.contents VALUES (2, 'Akka HTTP RTJVM.', 'Akka HTTP course from RTJVM.', 28,1);
INSERT INTO public.contents VALUES (4, 'Reactive Architecture(2) : Domain Driven Design', 'Domain Driven Design, Lightbend academy', 1,2);
INSERT INTO public.contents VALUES (1, 'Akka persistence classic, RTJVM.', 'Akka persistence using classic actors, from RTJVM', 20,1);
INSERT INTO public.contents VALUES (3, 'Reactive Architecture(1) : Introduction to Reacticve Systems', 'Introduction to Reactive System, Lightbend academy', 1,2);



