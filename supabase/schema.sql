-- Minecraft Journal Supabase Schema

create table if not exists worlds (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users not null default auth.uid(),
  name text not null,
  hardcore boolean default false,
  biome text default 'plains',
  created_at timestamptz default now()
);

create table if not exists entries (
  id uuid primary key default gen_random_uuid(),
  world_id uuid references worlds on delete cascade not null,
  title text,
  body text,
  tags text[],
  created_at timestamptz default now()
);

alter table worlds enable row level security;
alter table entries enable row level security;

create policy "own worlds" on worlds
  for all using (auth.uid() = user_id);

create policy "own entries" on entries
  for all using (
    world_id in (select id from worlds where user_id = auth.uid())
  );
