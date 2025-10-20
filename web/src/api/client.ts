/**
 * APIクライアント
 * 
 * バックエンドAPIとの通信を担当するモジュールです。
 * 
 * 初心者向け解説:
 * このファイルは、フロントエンド（ブラウザ）からバックエンド（サーバー）に
 * HTTPリクエストを送信するための関数をまとめたものです。
 * 
 * 例えば:
 * - ログイン: POST /api/auth/login
 * - タスク一覧取得: GET /api/tasks/my
 * - タスク作成: POST /api/tasks
 * 
 * axiosというライブラリを使って、HTTPリクエストを簡単に送信できます。
 */

import axios, { AxiosInstance, AxiosError } from 'axios';


/**
 * ユーザー情報
 */
export interface UserInfo {
  userId: number;
  userEmail: string;
  userFullName: string;
  userDisplayName?: string;
  userProfileImageUrl?: string;
  userTimezone: string;
  userLanguagePreference: string;
}

/**
 * ログインリクエスト
 */
export interface LoginRequest {
  userEmail: string;
  plainTextPassword: string;
}

/**
 * ユーザー登録リクエスト
 */
export interface RegisterRequest {
  userEmail: string;
  userFullName: string;
  plainTextPassword: string;
  userDisplayName?: string;
}

/**
 * ログインレスポンス
 */
export interface LoginResponse {
  jwtToken: string;
  userInfo: UserInfo;
  sessionExpiresAt: number;
}

/**
 * プロジェクト情報
 */
export interface Project {
  projectId: number;
  projectName: string;
  projectDescription?: string;
  parentWorkspaceId: number;
  projectOwnerUserId: number;
  projectColorCode: string;
  projectIconName?: string;
  projectStartDate?: string;
  projectDueDate?: string;
  projectCurrentStatus: string;
  projectCreatedAt: number;
  projectUpdatedAt: number;
  isProjectArchived: boolean;
  projectArchivedAt?: number;
  projectDeletedAt?: number;
}

/**
 * プロジェクト作成リクエスト
 */
export interface CreateProjectRequest {
  projectName: string;
  projectDescription?: string;
  parentWorkspaceId: number;
  projectColorCode?: string;
  projectStartDate?: string;
  projectDueDate?: string;
}

/**
 * タスク情報
 */
export interface Task {
  taskId: number;
  taskTitle: string;
  taskDescription?: string;
  parentProjectId: number;
  parentSectionId?: number;
  parentTaskId?: number;
  assignedUserId?: number;
  taskDueDateTime?: number;
  taskStartDate?: string;
  priorityLevel: number;
  currentTaskStatus: string;
  isTaskCompleted: boolean;
  taskCompletedAt?: number;
  taskCompletedByUserId?: number;
  taskDisplayOrder: number;
  taskCreatedAt: number;
  taskCreatedByUserId: number;
  taskUpdatedAt: number;
  taskUpdatedByUserId?: number;
  taskDeletedAt?: number;
}

/**
 * タスク作成リクエスト
 */
export interface CreateTaskRequest {
  taskTitle: string;
  taskDescription?: string;
  parentProjectId: number;
  parentSectionId?: number;
  parentTaskId?: number;
  assignedUserId?: number;
  taskDueDateTime?: number;
  priorityLevel?: number;
}

/**
 * タスク更新リクエスト
 */
export interface UpdateTaskRequest {
  taskTitle?: string;
  taskDescription?: string;
  parentSectionId?: number;
  assignedUserId?: number;
  taskDueDateTime?: number;
  priorityLevel?: number;
  currentTaskStatus?: string;
}

/**
 * エラーレスポンス
 */
export interface ErrorResponse {
  errorCode: string;
  errorMessage: string;
  errorDetails?: string;
  errorOccurredAt: number;
}


/**
 * APIクライアント
 * 
 * バックエンドAPIとの通信を担当するクラスです。
 */
class ApiClient {
  private axiosInstance: AxiosInstance;
  private authToken: string | null = null;

  constructor(baseURL: string = import.meta.env.VITE_API_BASE_URL || '/api') {
    this.axiosInstance = axios.create({
      baseURL,
      timeout: 30000, // 30秒
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.axiosInstance.interceptors.request.use(
      (config) => {
        if (this.authToken) {
          config.headers.Authorization = `Bearer ${this.authToken}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ErrorResponse>) => {
        if (error.response?.status === 401) {
          this.clearAuthToken();
          if (window.location.pathname !== '/login') {
            window.location.href = '/login';
          }
        }
        return Promise.reject(error);
      }
    );

    this.loadAuthTokenFromStorage();
  }

  /**
   * 認証トークンを設定
   */
  setAuthToken(token: string): void {
    this.authToken = token;
    localStorage.setItem('authToken', token);
  }

  /**
   * 認証トークンをクリア
   */
  clearAuthToken(): void {
    this.authToken = null;
    localStorage.removeItem('authToken');
  }

  /**
   * ローカルストレージからトークンを読み込み
   */
  private loadAuthTokenFromStorage(): void {
    const token = localStorage.getItem('authToken');
    if (token) {
      this.authToken = token;
    }
  }

  /**
   * 認証トークンが設定されているかチェック
   */
  isAuthenticated(): boolean {
    return this.authToken !== null;
  }


  /**
   * ユーザー登録
   */
  async register(request: RegisterRequest): Promise<LoginResponse> {
    const response = await this.axiosInstance.post<LoginResponse>('/auth/register', request);
    this.setAuthToken(response.data.jwtToken);
    return response.data;
  }

  /**
   * ログイン
   */
  async login(request: LoginRequest): Promise<LoginResponse> {
    const response = await this.axiosInstance.post<LoginResponse>('/auth/login', request);
    this.setAuthToken(response.data.jwtToken);
    return response.data;
  }

  /**
   * ログアウト
   */
  async logout(): Promise<void> {
    await this.axiosInstance.post('/auth/logout');
    this.clearAuthToken();
  }

  /**
   * 現在のユーザー情報を取得
   */
  async getCurrentUser(): Promise<UserInfo> {
    const response = await this.axiosInstance.get<UserInfo>('/auth/me');
    return response.data;
  }


  /**
   * プロジェクトを作成
   */
  async createProject(request: CreateProjectRequest): Promise<Project> {
    const response = await this.axiosInstance.post<Project>('/projects', request);
    return response.data;
  }

  /**
   * 自分が参加しているプロジェクト一覧を取得
   */
  async getMyProjects(): Promise<Project[]> {
    const response = await this.axiosInstance.get<Project[]>('/projects/my');
    return response.data;
  }

  /**
   * ワークスペースのプロジェクト一覧を取得
   */
  async getProjectsInWorkspace(workspaceId: number): Promise<Project[]> {
    const response = await this.axiosInstance.get<Project[]>(`/projects/workspace/${workspaceId}`);
    return response.data;
  }


  /**
   * タスクを作成
   */
  async createTask(request: CreateTaskRequest): Promise<Task> {
    const response = await this.axiosInstance.post<Task>('/tasks', request);
    return response.data;
  }

  /**
   * 自分に割り当てられたタスク一覧を取得
   */
  async getMyTasks(): Promise<Task[]> {
    const response = await this.axiosInstance.get<Task[]>('/tasks/my');
    return response.data;
  }

  /**
   * プロジェクトのタスク一覧を取得
   */
  async getTasksInProject(projectId: number): Promise<Task[]> {
    const response = await this.axiosInstance.get<Task[]>(`/tasks/project/${projectId}`);
    return response.data;
  }

  /**
   * タスクを更新
   */
  async updateTask(taskId: number, request: UpdateTaskRequest): Promise<Task> {
    const response = await this.axiosInstance.put<Task>(`/tasks/${taskId}`, request);
    return response.data;
  }

  /**
   * タスクを完了
   */
  async completeTask(taskId: number): Promise<Task> {
    const response = await this.axiosInstance.post<Task>(`/tasks/${taskId}/complete`);
    return response.data;
  }

  /**
   * タスクを削除
   */
  async deleteTask(taskId: number): Promise<void> {
    await this.axiosInstance.delete(`/tasks/${taskId}`);
  }

  /**
   * タスクを検索
   */
  async searchTasks(params: {
    query?: string;
    assignedUserId?: number;
    projectId?: number;
    status?: string;
    priority?: number;
  }): Promise<Task[]> {
    const response = await this.axiosInstance.get<Task[]>('/tasks', { params });
    return response.data;
  }
}

export const apiClient = new ApiClient();
