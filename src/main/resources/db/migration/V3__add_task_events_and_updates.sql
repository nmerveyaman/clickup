-- 1. Önce kullanıcılar tablosu (Eksikti, eklendi)
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT PRIMARY KEY NOT NULL,
                                     username VARCHAR(255),
    email VARCHAR(255),
    profile_picture TEXT
    );

-- 2. Workspaces
CREATE TABLE IF NOT EXISTS workspaces (
                                          id VARCHAR(255) PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL
    );

-- 3. Workspace Members
CREATE TABLE IF NOT EXISTS workspace_members (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 workspace_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    role_key VARCHAR(255),
    invited_by_user_id BIGINT,
    date_joined VARCHAR(255),
    date_invited VARCHAR(255),
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES users(id)
    );

-- 4. Spaces
CREATE TABLE IF NOT EXISTS spaces (
                                      id VARCHAR(255) PRIMARY KEY,
    workspace_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    is_private BOOLEAN,
    is_archived BOOLEAN,
    CONSTRAINT fk_spaces_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
    );

-- 5. Folders
CREATE TABLE IF NOT EXISTS folders (
                                       id VARCHAR(255) PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    space_id VARCHAR(255) NOT NULL,
    hidden BOOLEAN
    );

-- 6. Lists
CREATE TABLE IF NOT EXISTS lists (
                                     id VARCHAR(255) PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    space_id VARCHAR(255) NOT NULL,
    folder_id VARCHAR(255),
    task_count INTEGER
    );

-- 7. Tasks (Priority ve blocked_by_task_id alanları dahil)
CREATE TABLE IF NOT EXISTS tasks (
                                     id VARCHAR(255) PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    status_name VARCHAR(255),
    status_type VARCHAR(255),
    order_index VARCHAR(255),
    date_created VARCHAR(255),
    date_updated VARCHAR(255),
    date_closed VARCHAR(255),
    date_done VARCHAR(255),
    creator_id VARCHAR(255),
    list_id VARCHAR(255),
    priority VARCHAR(50),
    blocked_by_task_id VARCHAR(255)
    );

-- 8. Task Events (from_assignee, to_assignee ve IDENTITY uyumlu BIGSERIAL id)
CREATE TABLE IF NOT EXISTS task_events (
                                           id BIGSERIAL PRIMARY KEY,
                                           task_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    from_status VARCHAR(255),
    to_status VARCHAR(255),
    from_assignee BIGINT,
    to_assignee BIGINT,
    changed_by_user_id BIGINT,
    timestamp VARCHAR(255),
    CONSTRAINT fk_task_events_task FOREIGN KEY (task_id) REFERENCES tasks(id)
    );

-- 9. Task Assignees (Ara Tablo)
CREATE TABLE IF NOT EXISTS task_assignees (
                                              task_id VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, user_id),
    CONSTRAINT fk_task_assignees_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_assignees_user FOREIGN KEY (user_id) REFERENCES users(id)
    );