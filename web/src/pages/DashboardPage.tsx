import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient, UserInfo, Task } from '../api/client';

interface DashboardPageProps {
  user: UserInfo;
}

export default function DashboardPage({ user }: DashboardPageProps) {
  const [myTasks, setMyTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadMyTasks();
  }, []);

  const loadMyTasks = async () => {
    try {
      const tasks = await apiClient.getMyTasks();
      setMyTasks(tasks);
    } catch (error) {
      console.error('タスクの取得に失敗しました:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await apiClient.logout();
      window.location.href = '/login';
    } catch (error) {
      console.error('ログアウトに失敗しました:', error);
    }
  };

  const getPriorityLabel = (priority: number) => {
    switch (priority) {
      case 4: return '緊急';
      case 3: return '高';
      case 2: return '中';
      case 1: return '低';
      default: return '中';
    }
  };

  const getPriorityColor = (priority: number) => {
    switch (priority) {
      case 4: return 'bg-red-100 text-red-800';
      case 3: return 'bg-orange-100 text-orange-800';
      case 2: return 'bg-yellow-100 text-yellow-800';
      case 1: return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* ヘッダー */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">ダッシュボード</h1>
              <p className="text-sm text-gray-600">ようこそ、{user.userFullName}さん</p>
            </div>
            <div className="flex space-x-4">
              <button
                onClick={() => navigate('/projects')}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                プロジェクト
              </button>
              <button
                onClick={() => navigate('/tasks')}
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
              >
                タスク
              </button>
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                ログアウト
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* メインコンテンツ */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">自分のタスク</h2>
          
          {isLoading ? (
            <div className="text-center py-12">
              <p className="text-gray-600">読み込み中...</p>
            </div>
          ) : myTasks.length === 0 ? (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-600">割り当てられたタスクはありません</p>
              <button
                onClick={() => navigate('/tasks')}
                className="mt-4 px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
              >
                タスクを作成
              </button>
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow overflow-hidden">
              <ul className="divide-y divide-gray-200">
                {myTasks.map((task) => (
                  <li key={task.taskId} className="p-4 hover:bg-gray-50">
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <h3 className="text-sm font-medium text-gray-900">{task.taskTitle}</h3>
                        {task.taskDescription && (
                          <p className="mt-1 text-sm text-gray-600">{task.taskDescription}</p>
                        )}
                        <div className="mt-2 flex items-center space-x-2">
                          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getPriorityColor(task.priorityLevel)}`}>
                            {getPriorityLabel(task.priorityLevel)}
                          </span>
                          <span className="text-xs text-gray-500">
                            {task.currentTaskStatus === 'not_started' && '未着手'}
                            {task.currentTaskStatus === 'in_progress' && '進行中'}
                            {task.currentTaskStatus === 'waiting' && '待機中'}
                            {task.currentTaskStatus === 'completed' && '完了'}
                          </span>
                          {task.taskDueDateTime && (
                            <span className="text-xs text-gray-500">
                              期限: {new Date(task.taskDueDateTime).toLocaleDateString('ja-JP')}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
