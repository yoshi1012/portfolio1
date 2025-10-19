-- 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";  -- UUID生成用


CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL UNIQUE,
    user_full_name VARCHAR(255) NOT NULL,
    user_display_name VARCHAR(100),
    hashed_password_value VARCHAR(255) NOT NULL,
    user_profile_image_url TEXT,
    user_timezone VARCHAR(50) DEFAULT 'UTC',
    user_language_preference VARCHAR(10) DEFAULT 'ja',
    account_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    account_last_login_at TIMESTAMP,
    is_account_active BOOLEAN NOT NULL DEFAULT true,
    account_deleted_at TIMESTAMP,
    
    CONSTRAINT unique_active_user_email UNIQUE (user_email)
);

CREATE INDEX idx_users_email ON users(user_email) WHERE account_deleted_at IS NULL;
CREATE INDEX idx_users_active ON users(is_account_active) WHERE account_deleted_at IS NULL;

COMMENT ON TABLE users IS 'アプリケーションの全ユーザー情報';
COMMENT ON COLUMN users.user_id IS 'ユーザーの一意な識別子';
COMMENT ON COLUMN users.user_email IS 'ログインに使用するメールアドレス';
COMMENT ON COLUMN users.user_full_name IS 'ユーザーの本名（例: 山田太郎）';
COMMENT ON COLUMN users.user_display_name IS '表示名（例: やまだ）';
COMMENT ON COLUMN users.hashed_password_value IS 'BCryptでハッシュ化されたパスワード';


CREATE TABLE organizations (
    organization_id BIGSERIAL PRIMARY KEY,
    organization_name VARCHAR(255) NOT NULL,
    organization_description TEXT,
    organization_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    organization_owner_user_id BIGINT NOT NULL REFERENCES users(user_id),
    is_organization_active BOOLEAN NOT NULL DEFAULT true,
    organization_deleted_at TIMESTAMP
);

CREATE INDEX idx_organizations_owner ON organizations(organization_owner_user_id);

COMMENT ON TABLE organizations IS '会社や大きな組織の単位';
COMMENT ON COLUMN organizations.organization_owner_user_id IS 'この組織を作成したオーナー';

CREATE TABLE organization_members (
    organization_member_id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(organization_id) ON DELETE CASCADE,
    member_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    member_role_in_organization VARCHAR(50) NOT NULL DEFAULT 'member',
    member_joined_organization_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    member_left_organization_at TIMESTAMP,
    
    CONSTRAINT unique_organization_member UNIQUE (organization_id, member_user_id)
);

CREATE INDEX idx_org_members_org ON organization_members(organization_id);
CREATE INDEX idx_org_members_user ON organization_members(member_user_id);

COMMENT ON TABLE organization_members IS '組織とユーザーの所属関係';
COMMENT ON COLUMN organization_members.member_role_in_organization IS 'owner=オーナー, admin=管理者, member=一般メンバー';

CREATE TABLE workspaces (
    workspace_id BIGSERIAL PRIMARY KEY,
    workspace_name VARCHAR(255) NOT NULL,
    workspace_description TEXT,
    parent_organization_id BIGINT NOT NULL REFERENCES organizations(organization_id) ON DELETE CASCADE,
    workspace_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    workspace_creator_user_id BIGINT NOT NULL REFERENCES users(user_id),
    is_workspace_active BOOLEAN NOT NULL DEFAULT true,
    workspace_deleted_at TIMESTAMP
);

CREATE INDEX idx_workspaces_organization ON workspaces(parent_organization_id);
CREATE INDEX idx_workspaces_creator ON workspaces(workspace_creator_user_id);

COMMENT ON TABLE workspaces IS 'チームや部門の単位';
COMMENT ON COLUMN workspaces.parent_organization_id IS 'このワークスペースが属する組織';

CREATE TABLE workspace_members (
    workspace_member_id BIGSERIAL PRIMARY KEY,
    workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    member_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    member_role_in_workspace VARCHAR(50) NOT NULL DEFAULT 'member',
    member_joined_workspace_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    member_left_workspace_at TIMESTAMP,
    
    CONSTRAINT unique_workspace_member UNIQUE (workspace_id, member_user_id)
);

CREATE INDEX idx_workspace_members_workspace ON workspace_members(workspace_id);
CREATE INDEX idx_workspace_members_user ON workspace_members(member_user_id);

COMMENT ON TABLE workspace_members IS 'ワークスペースとユーザーの所属関係';


CREATE TABLE projects (
    project_id BIGSERIAL PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    project_description TEXT,
    parent_workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    project_owner_user_id BIGINT NOT NULL REFERENCES users(user_id),
    project_color_code VARCHAR(7) DEFAULT '#4A90E2',  -- 例: #FF5733
    project_icon_name VARCHAR(50),  -- 例: 'folder', 'star', 'rocket'
    project_start_date DATE,
    project_due_date DATE,
    project_current_status VARCHAR(50) NOT NULL DEFAULT 'on_track',
    project_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    project_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_project_archived BOOLEAN NOT NULL DEFAULT false,
    project_archived_at TIMESTAMP,
    project_deleted_at TIMESTAMP
);

CREATE INDEX idx_projects_workspace ON projects(parent_workspace_id);
CREATE INDEX idx_projects_owner ON projects(project_owner_user_id);
CREATE INDEX idx_projects_status ON projects(project_current_status);
CREATE INDEX idx_projects_due_date ON projects(project_due_date);

COMMENT ON TABLE projects IS '具体的なプロジェクト';
COMMENT ON COLUMN projects.project_current_status IS 'on_track=順調, at_risk=注意, off_track=遅延, completed=完了, on_hold=保留';
COMMENT ON COLUMN projects.project_color_code IS 'プロジェクトの識別色（16進数カラーコード）';

CREATE TABLE project_members (
    project_member_id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    member_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    member_role_in_project VARCHAR(50) NOT NULL DEFAULT 'member',
    member_joined_project_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    member_left_project_at TIMESTAMP,
    
    CONSTRAINT unique_project_member UNIQUE (project_id, member_user_id)
);

CREATE INDEX idx_project_members_project ON project_members(project_id);
CREATE INDEX idx_project_members_user ON project_members(member_user_id);

COMMENT ON TABLE project_members IS 'プロジェクトとユーザーの参加関係';
COMMENT ON COLUMN project_members.member_role_in_project IS 'owner=オーナー, editor=編集者, commenter=コメント可, viewer=閲覧のみ';

CREATE TABLE sections (
    section_id BIGSERIAL PRIMARY KEY,
    section_name VARCHAR(255) NOT NULL,
    parent_project_id BIGINT NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    section_display_order INTEGER NOT NULL DEFAULT 0,
    section_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    section_deleted_at TIMESTAMP
);

CREATE INDEX idx_sections_project ON sections(parent_project_id);
CREATE INDEX idx_sections_order ON sections(parent_project_id, section_display_order);

COMMENT ON TABLE sections IS 'プロジェクト内のグループ分け（カンバンの列のようなもの）';
COMMENT ON COLUMN sections.section_display_order IS '表示順序（小さい数字ほど上に表示）';


CREATE TABLE tasks (
    task_id BIGSERIAL PRIMARY KEY,
    task_title VARCHAR(500) NOT NULL,
    task_description TEXT,
    parent_project_id BIGINT NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    parent_section_id BIGINT REFERENCES sections(section_id) ON DELETE SET NULL,
    parent_task_id BIGINT REFERENCES tasks(task_id) ON DELETE CASCADE,  -- サブタスクの場合
    
    assigned_user_id BIGINT REFERENCES users(user_id) ON DELETE SET NULL,
    task_due_date_time TIMESTAMP,
    task_start_date DATE,
    
    priority_level INTEGER NOT NULL DEFAULT 2,
    current_task_status VARCHAR(50) NOT NULL DEFAULT 'not_started',
    
    is_task_completed BOOLEAN NOT NULL DEFAULT false,
    task_completed_at TIMESTAMP,
    task_completed_by_user_id BIGINT REFERENCES users(user_id),
    
    task_display_order INTEGER NOT NULL DEFAULT 0,
    
    task_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    task_created_by_user_id BIGINT NOT NULL REFERENCES users(user_id),
    task_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    task_updated_by_user_id BIGINT REFERENCES users(user_id),
    task_deleted_at TIMESTAMP
);

CREATE INDEX idx_tasks_project ON tasks(parent_project_id);
CREATE INDEX idx_tasks_section ON tasks(parent_section_id);
CREATE INDEX idx_tasks_parent ON tasks(parent_task_id);
CREATE INDEX idx_tasks_assignee ON tasks(assigned_user_id);
CREATE INDEX idx_tasks_due_date ON tasks(task_due_date_time);
CREATE INDEX idx_tasks_status ON tasks(current_task_status);
CREATE INDEX idx_tasks_priority ON tasks(priority_level);
CREATE INDEX idx_tasks_completed ON tasks(is_task_completed);

COMMENT ON TABLE tasks IS '実際の作業単位';
COMMENT ON COLUMN tasks.parent_task_id IS 'サブタスクの場合、親タスクのID';
COMMENT ON COLUMN tasks.priority_level IS '1=低, 2=中, 3=高, 4=緊急';
COMMENT ON COLUMN tasks.current_task_status IS 'not_started=未着手, in_progress=進行中, waiting=待機中, completed=完了, cancelled=キャンセル';

CREATE TABLE task_dependencies (
    task_dependency_id BIGSERIAL PRIMARY KEY,
    dependent_task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    prerequisite_task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    dependency_type VARCHAR(50) NOT NULL DEFAULT 'finish_to_start',
    dependency_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_task_dependency UNIQUE (dependent_task_id, prerequisite_task_id),
    CONSTRAINT no_self_dependency CHECK (dependent_task_id != prerequisite_task_id)
);

CREATE INDEX idx_task_deps_dependent ON task_dependencies(dependent_task_id);
CREATE INDEX idx_task_deps_prerequisite ON task_dependencies(prerequisite_task_id);

COMMENT ON TABLE task_dependencies IS 'タスク間の依存関係';
COMMENT ON COLUMN task_dependencies.dependent_task_id IS '依存する側のタスク（後で実行するタスク）';
COMMENT ON COLUMN task_dependencies.prerequisite_task_id IS '前提となるタスク（先に完了すべきタスク）';
COMMENT ON COLUMN task_dependencies.dependency_type IS 'finish_to_start=完了→開始（最も一般的）';

CREATE TABLE tags (
    tag_id BIGSERIAL PRIMARY KEY,
    tag_name VARCHAR(100) NOT NULL,
    tag_color_code VARCHAR(7) DEFAULT '#808080',
    parent_workspace_id BIGINT NOT NULL REFERENCES workspaces(workspace_id) ON DELETE CASCADE,
    tag_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tag_deleted_at TIMESTAMP,
    
    CONSTRAINT unique_tag_name_per_workspace UNIQUE (parent_workspace_id, tag_name)
);

CREATE INDEX idx_tags_workspace ON tags(parent_workspace_id);

COMMENT ON TABLE tags IS 'タスクに付けるラベル';

CREATE TABLE task_tags (
    task_tag_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(tag_id) ON DELETE CASCADE,
    tag_attached_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_task_tag UNIQUE (task_id, tag_id)
);

CREATE INDEX idx_task_tags_task ON task_tags(task_id);
CREATE INDEX idx_task_tags_tag ON task_tags(tag_id);

COMMENT ON TABLE task_tags IS 'タスクとタグの関連';


CREATE TABLE comments (
    comment_id BIGSERIAL PRIMARY KEY,
    parent_task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    comment_author_user_id BIGINT NOT NULL REFERENCES users(user_id),
    comment_text_content TEXT NOT NULL,
    comment_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    comment_updated_at TIMESTAMP,
    comment_deleted_at TIMESTAMP
);

CREATE INDEX idx_comments_task ON comments(parent_task_id);
CREATE INDEX idx_comments_author ON comments(comment_author_user_id);
CREATE INDEX idx_comments_created ON comments(comment_created_at);

COMMENT ON TABLE comments IS 'タスクに対するコメント';

CREATE TABLE attachments (
    attachment_id BIGSERIAL PRIMARY KEY,
    parent_task_id BIGINT NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE,
    attachment_file_name VARCHAR(255) NOT NULL,
    attachment_file_url TEXT NOT NULL,
    attachment_file_size_bytes BIGINT,
    attachment_mime_type VARCHAR(100),
    uploaded_by_user_id BIGINT NOT NULL REFERENCES users(user_id),
    attachment_uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attachment_deleted_at TIMESTAMP
);

CREATE INDEX idx_attachments_task ON attachments(parent_task_id);
CREATE INDEX idx_attachments_uploader ON attachments(uploaded_by_user_id);

COMMENT ON TABLE attachments IS 'タスクに添付されたファイル';
COMMENT ON COLUMN attachments.attachment_file_size_bytes IS 'ファイルサイズ（バイト単位）';
COMMENT ON COLUMN attachments.attachment_mime_type IS 'ファイルの種類（例: image/png, application/pdf）';


CREATE TABLE notifications (
    notification_id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    notification_type VARCHAR(50) NOT NULL,
    related_task_id BIGINT REFERENCES tasks(task_id) ON DELETE CASCADE,
    related_project_id BIGINT REFERENCES projects(project_id) ON DELETE CASCADE,
    related_comment_id BIGINT REFERENCES comments(comment_id) ON DELETE CASCADE,
    notification_message TEXT NOT NULL,
    is_notification_read BOOLEAN NOT NULL DEFAULT false,
    notification_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notification_read_at TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_user_id);
CREATE INDEX idx_notifications_read ON notifications(is_notification_read);
CREATE INDEX idx_notifications_created ON notifications(notification_created_at);
CREATE INDEX idx_notifications_task ON notifications(related_task_id);

COMMENT ON TABLE notifications IS 'ユーザーへの通知';
COMMENT ON COLUMN notifications.notification_type IS 'task_assigned=タスク割り当て, comment_added=コメント追加, due_date_approaching=期限接近など';


CREATE TABLE activity_logs (
    activity_log_id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT NOT NULL REFERENCES users(user_id),
    activity_type VARCHAR(50) NOT NULL,
    related_task_id BIGINT REFERENCES tasks(task_id) ON DELETE SET NULL,
    related_project_id BIGINT REFERENCES projects(project_id) ON DELETE SET NULL,
    related_workspace_id BIGINT REFERENCES workspaces(workspace_id) ON DELETE SET NULL,
    activity_description TEXT NOT NULL,
    activity_metadata JSONB,  -- 追加の詳細情報（変更前後の値など）
    activity_occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_logs_actor ON activity_logs(actor_user_id);
CREATE INDEX idx_activity_logs_type ON activity_logs(activity_type);
CREATE INDEX idx_activity_logs_task ON activity_logs(related_task_id);
CREATE INDEX idx_activity_logs_project ON activity_logs(related_project_id);
CREATE INDEX idx_activity_logs_occurred ON activity_logs(activity_occurred_at);

COMMENT ON TABLE activity_logs IS '全ての変更履歴';
COMMENT ON COLUMN activity_logs.activity_metadata IS 'JSON形式で追加情報を保存（例: {"old_status": "in_progress", "new_status": "completed"}）';


CREATE TABLE user_sessions (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    jwt_token_value TEXT NOT NULL,
    session_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_expires_at TIMESTAMP NOT NULL,
    session_last_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_ip_address VARCHAR(45),
    user_agent_string TEXT
);

CREATE INDEX idx_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_sessions_expires ON user_sessions(session_expires_at);

COMMENT ON TABLE user_sessions IS 'ログイン中のユーザーのセッション情報';
COMMENT ON COLUMN user_sessions.jwt_token_value IS 'JWT認証トークン';
COMMENT ON COLUMN user_sessions.user_ip_address IS 'ユーザーのIPアドレス';
COMMENT ON COLUMN user_sessions.user_agent_string IS 'ブラウザやアプリの情報';


INSERT INTO users (user_email, user_full_name, user_display_name, hashed_password_value) VALUES
('admin@example.com', '管理者ユーザー', '管理者', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
('user1@example.com', '山田太郎', 'やまだ', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
('user2@example.com', '佐藤花子', 'さとう', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

INSERT INTO organizations (organization_name, organization_description, organization_owner_user_id) VALUES
('サンプル株式会社', 'テスト用の組織です', 1);

INSERT INTO workspaces (workspace_name, workspace_description, parent_organization_id, workspace_creator_user_id) VALUES
('開発チーム', 'ソフトウェア開発チームのワークスペース', 1, 1),
('営業チーム', '営業部門のワークスペース', 1, 1);

INSERT INTO organization_members (organization_id, member_user_id, member_role_in_organization) VALUES
(1, 1, 'owner'),
(1, 2, 'admin'),
(1, 3, 'member');

INSERT INTO workspace_members (workspace_id, member_user_id, member_role_in_workspace) VALUES
(1, 1, 'admin'),
(1, 2, 'member'),
(2, 1, 'admin'),
(2, 3, 'member');

INSERT INTO projects (project_name, project_description, parent_workspace_id, project_owner_user_id, project_color_code, project_current_status) VALUES
('Webサイトリニューアル', '会社のWebサイトを新しくするプロジェクト', 1, 1, '#4A90E2', 'on_track'),
('新商品開発', '新しい商品を開発するプロジェクト', 1, 1, '#E24A90', 'at_risk');

INSERT INTO project_members (project_id, member_user_id, member_role_in_project) VALUES
(1, 1, 'owner'),
(1, 2, 'editor'),
(2, 1, 'owner'),
(2, 3, 'editor');

INSERT INTO sections (section_name, parent_project_id, section_display_order) VALUES
('未着手', 1, 1),
('進行中', 1, 2),
('レビュー待ち', 1, 3),
('完了', 1, 4);

INSERT INTO tasks (task_title, task_description, parent_project_id, parent_section_id, assigned_user_id, priority_level, current_task_status, task_created_by_user_id) VALUES
('デザインモックアップを作成', 'Figmaで新しいデザインのモックアップを作成する', 1, 1, 2, 3, 'not_started', 1),
('既存サイトの分析', '現在のWebサイトのアクセス解析とユーザー行動を分析する', 1, 2, 2, 2, 'in_progress', 1),
('コンテンツの執筆', '新しいページのコンテンツを執筆する', 1, 1, 3, 2, 'not_started', 1);

INSERT INTO tags (tag_name, tag_color_code, parent_workspace_id) VALUES
('緊急', '#FF0000', 1),
('デザイン', '#9C27B0', 1),
('開発', '#2196F3', 1),
('営業', '#4CAF50', 1);

INSERT INTO task_tags (task_id, tag_id) VALUES
(1, 2),  -- デザインモックアップ → デザインタグ
(2, 3);  -- 既存サイトの分析 → 開発タグ

INSERT INTO comments (parent_task_id, comment_author_user_id, comment_text_content) VALUES
(2, 1, 'Google Analyticsのデータを確認しました。モバイルからのアクセスが60%を超えています。'),
(2, 2, 'ありがとうございます。モバイルファーストでデザインを進めます。');


CREATE VIEW active_tasks_view AS
SELECT 
    t.task_id,
    t.task_title,
    t.task_description,
    t.priority_level,
    t.current_task_status,
    t.task_due_date_time,
    t.assigned_user_id,
    u.user_full_name AS assigned_user_name,
    p.project_id,
    p.project_name,
    s.section_name,
    t.task_created_at
FROM tasks t
LEFT JOIN users u ON t.assigned_user_id = u.user_id
LEFT JOIN projects p ON t.parent_project_id = p.project_id
LEFT JOIN sections s ON t.parent_section_id = s.section_id
WHERE t.task_deleted_at IS NULL
  AND t.is_task_completed = false;

COMMENT ON VIEW active_tasks_view IS '削除されていない、未完了のタスク一覧';

CREATE VIEW user_task_counts_view AS
SELECT 
    u.user_id,
    u.user_full_name,
    COUNT(CASE WHEN t.current_task_status = 'not_started' THEN 1 END) AS not_started_count,
    COUNT(CASE WHEN t.current_task_status = 'in_progress' THEN 1 END) AS in_progress_count,
    COUNT(CASE WHEN t.current_task_status = 'completed' THEN 1 END) AS completed_count,
    COUNT(*) AS total_task_count
FROM users u
LEFT JOIN tasks t ON u.user_id = t.assigned_user_id AND t.task_deleted_at IS NULL
WHERE u.account_deleted_at IS NULL
GROUP BY u.user_id, u.user_full_name;

COMMENT ON VIEW user_task_counts_view IS 'ユーザーごとのタスク数の集計';


CREATE OR REPLACE FUNCTION update_task_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.task_updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_task_timestamp
BEFORE UPDATE ON tasks
FOR EACH ROW
EXECUTE FUNCTION update_task_updated_at();

COMMENT ON FUNCTION update_task_updated_at() IS 'タスクが更新されたときに task_updated_at を自動更新';

CREATE OR REPLACE FUNCTION update_project_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.project_updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_project_timestamp
BEFORE UPDATE ON projects
FOR EACH ROW
EXECUTE FUNCTION update_project_updated_at();


CREATE OR REPLACE FUNCTION get_user_pending_tasks(target_user_id BIGINT)
RETURNS TABLE (
    task_id BIGINT,
    task_title VARCHAR,
    project_name VARCHAR,
    task_due_date_time TIMESTAMP,
    priority_level INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.task_id,
        t.task_title,
        p.project_name,
        t.task_due_date_time,
        t.priority_level
    FROM tasks t
    JOIN projects p ON t.parent_project_id = p.project_id
    WHERE t.assigned_user_id = target_user_id
      AND t.is_task_completed = false
      AND t.task_deleted_at IS NULL
    ORDER BY t.task_due_date_time ASC NULLS LAST, t.priority_level DESC;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION get_user_pending_tasks(BIGINT) IS '特定のユーザーの未完了タスクを期限順に取得';
