import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient, UserInfo, Task } from '../api/client';

interface TasksPageProps {
  user: UserInfo;
}

export default function TasksPage({ user }: TasksPageProps) {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadTasks();
  }, []);

  const loadTasks = async () => {
    try {
      const taskList = await apiClient.getMyTasks();
      setTasks(taskList);
    } catch (error) {
      console.error('タスクの取得に失敗しました:', error);
    } finally {
      setIsLoading(false);
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

  const handleCompleteTask = async (taskId: number) => {
    try {
      await apiClient.completeTask(taskId);
      loadTasks();
    } catch (error) {
      console.error('タスクの完了に失敗しました:', error);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* ヘッダー */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <h1 className="text-2xl font-bold text-gray-900">タスク</h1>
            <div className="flex space-x-4">
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                ダッシュボード
              </button>
              <button
                onClick={() => navigate('/projects')}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                プロジェクト
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* メインコンテンツ */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {isLoading ? (
          <div className="text-center py-12">
            <p className="text-gray-600">読み込み中...</p>
          </div>
        ) : tasks.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <p className="text-gray-600">タスクがありません</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <ul className="divide-y divide-gray-200">
              {tasks.map((task) => (
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
                    {!task.isTaskCompleted && (
                      <button
                        onClick={() => handleCompleteTask(task.taskId)}
                        className="ml-4 px-3 py-1 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700"
                      >
                        完了
                      </button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </main>
    </div>
  );
}
