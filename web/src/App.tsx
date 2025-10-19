/**
 * メインアプリケーションコンポーネント
 * 
 * 初心者向け解説:
 * このファイルは、Reactアプリケーションのルート（根っこ）となるコンポーネントです。
 * ルーティング（URLに応じて表示するページを切り替える）を設定しています。
 * 
 * 例えば:
 * - /login → ログインページ
 * - /dashboard → ダッシュボード
 * - /projects → プロジェクト一覧
 * - /tasks → タスク一覧
 */

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { apiClient, UserInfo } from './api/client';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import ProjectsPage from './pages/ProjectsPage';
import TasksPage from './pages/TasksPage';

function App() {
  const [currentUser, setCurrentUser] = useState<UserInfo | null>(null);
  const [isLoadingUser, setIsLoadingUser] = useState(true);

  useEffect(() => {
    const loadCurrentUser = async () => {
      if (apiClient.isAuthenticated()) {
        try {
          const user = await apiClient.getCurrentUser();
          setCurrentUser(user);
        } catch (error) {
          console.error('ユーザー情報の取得に失敗しました:', error);
          apiClient.clearAuthToken();
        }
      }
      setIsLoadingUser(false);
    };

    loadCurrentUser();
  }, []);

  if (isLoadingUser) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-xl text-gray-600">読み込み中...</div>
      </div>
    );
  }

  return (
    <BrowserRouter>
      <Routes>
        {/* 認証が必要なページ */}
        {currentUser ? (
          <>
            <Route path="/dashboard" element={<DashboardPage user={currentUser} />} />
            <Route path="/projects" element={<ProjectsPage user={currentUser} />} />
            <Route path="/tasks" element={<TasksPage user={currentUser} />} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/login" element={<Navigate to="/dashboard" replace />} />
            <Route path="/register" element={<Navigate to="/dashboard" replace />} />
          </>
        ) : (
          <>
            {/* 認証が不要なページ */}
            <Route path="/login" element={<LoginPage onLogin={setCurrentUser} />} />
            <Route path="/register" element={<RegisterPage onRegister={setCurrentUser} />} />
            <Route path="*" element={<Navigate to="/login" replace />} />
          </>
        )}
      </Routes>
    </BrowserRouter>
  );
}

export default App;
