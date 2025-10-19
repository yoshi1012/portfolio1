import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiClient, UserInfo, Project } from '../api/client';

interface ProjectsPageProps {
  user: UserInfo;
}

export default function ProjectsPage({ user }: ProjectsPageProps) {
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      const projectList = await apiClient.getMyProjects();
      setProjects(projectList);
    } catch (error) {
      console.error('プロジェクトの取得に失敗しました:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'on_track': return '順調';
      case 'at_risk': return '注意';
      case 'off_track': return '遅延';
      case 'completed': return '完了';
      case 'on_hold': return '保留';
      default: return status;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'on_track': return 'bg-green-100 text-green-800';
      case 'at_risk': return 'bg-yellow-100 text-yellow-800';
      case 'off_track': return 'bg-red-100 text-red-800';
      case 'completed': return 'bg-blue-100 text-blue-800';
      case 'on_hold': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* ヘッダー */}
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex justify-between items-center">
            <h1 className="text-2xl font-bold text-gray-900">プロジェクト</h1>
            <div className="flex space-x-4">
              <button
                onClick={() => navigate('/dashboard')}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                ダッシュボード
              </button>
              <button
                onClick={() => navigate('/tasks')}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                タスク
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
        ) : projects.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center">
            <p className="text-gray-600">プロジェクトがありません</p>
            <p className="text-sm text-gray-500 mt-2">
              プロジェクトを作成するには、ワークスペースが必要です
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {projects.map((project) => (
              <div
                key={project.projectId}
                className="bg-white rounded-lg shadow hover:shadow-lg transition-shadow cursor-pointer"
              >
                <div className="p-6">
                  <div className="flex items-center justify-between mb-4">
                    <div
                      className="w-3 h-3 rounded-full"
                      style={{ backgroundColor: project.projectColorCode }}
                    />
                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor(project.projectCurrentStatus)}`}>
                      {getStatusLabel(project.projectCurrentStatus)}
                    </span>
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    {project.projectName}
                  </h3>
                  {project.projectDescription && (
                    <p className="text-sm text-gray-600 mb-4 line-clamp-2">
                      {project.projectDescription}
                    </p>
                  )}
                  <div className="flex items-center justify-between text-xs text-gray-500">
                    {project.projectDueDate && (
                      <span>期限: {new Date(project.projectDueDate).toLocaleDateString('ja-JP')}</span>
                    )}
                    <span>更新: {new Date(project.projectUpdatedAt).toLocaleDateString('ja-JP')}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
