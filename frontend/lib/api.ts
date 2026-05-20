import axios, { AxiosInstance, AxiosError } from 'axios';

// Create Axios instance
const api: AxiosInstance = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('urbanops_token') : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);


api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      const url = String(error.config?.url ?? '');
      const isAuthAttempt = url.includes('/auth/login') || url.includes('/auth/register');
      if (!isAuthAttempt && typeof window !== 'undefined') {
        localStorage.removeItem('urbanops_token');
        localStorage.removeItem('urbanops_user');
        window.location.href = '/auth/signin';
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  login: (email: string, password: string) => 
    api.post('/auth/login', { email, password }),
  
  register: (data: {
    firstName: string;
    lastName: string;
    email: string;
    password: string;
    phone?: string;
    sector?: string;
    receiveAlerts?: boolean;
  }) => api.post('/auth/register', data),
  
  getMe: () => api.get('/auth/me'),
  
  logout: () => {
    localStorage.removeItem('urbanops_token');
    localStorage.removeItem('urbanops_user');
    return api.post('/auth/logout');
  },
};

// Incidents API
export const incidentAPI = {
  getAll: (params?: { categoryId?: number; sectorId?: number; severity?: string; status?: string; keyword?: string; page?: number; size?: number }) =>
    api.get('/incidents', { params }),
  
  getById: (id: number) => api.get(`/incidents/${id}`),
  
  getByReference: (code: string) => api.get(`/incidents/reference/${code}`),
  
  create: async (data: FormData) => {
    const headers: Record<string, string> = {};
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('urbanops_token');
      if (token) headers['Authorization'] = `Bearer ${token}`;
    }
    const res = await fetch(`${api.defaults.baseURL}/incidents`, {
      method: 'POST',
      headers,
      body: data,
    });
    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      throw { response: { data: errData } };
    }
    return { data: await res.json() } as any;
  },
  
  updateStatus: (id: number, status: string) => api.patch(`/incidents/${id}/status`, { status }),
  
  delete: (id: number) => api.delete(`/incidents/${id}`),
  
  getMyIncidents: () => api.get('/incidents/my'),
  
  getForMap: () => api.get('/incidents/map'),
  
  getRecent: () => api.get('/incidents/recent'),
  
  getBySector: (sectorId: number) => api.get(`/incidents/sector/${sectorId}`),
  
  getByCategory: (categoryId: number) => api.get(`/incidents/category/${categoryId}`),
};

// Alerts API
export const alertAPI = {
  getAll: (params?: { severity?: string; acknowledged?: boolean; page?: number; size?: number }) =>
    api.get('/alerts', { params }),
  
  getById: (id: number) => api.get(`/alerts/${id}`),
  
  getByIncident: (incidentId: number) => api.get(`/alerts/incident/${incidentId}`),
  
  resend: (incidentId: number) => api.post(`/alerts/${incidentId}/resend`),
  
  acknowledge: (id: number) => api.patch(`/alerts/${id}/acknowledge`),
  
  getRecent: () => api.get('/alerts/recent'),
  
  getCritical: () => api.get('/alerts/critical'),
};

// Stats API
export const statsAPI = {
  getDashboard: () => api.get('/stats/dashboard'),
  
  getByCategory: () => api.get('/stats/incidents/by-category'),
  
  getBySector: () => api.get('/stats/incidents/by-sector'),
  
  getHourly: () => api.get('/stats/incidents/hourly'),
  
  getServicesHealth: () => api.get('/stats/services/health'),
  
  getResolutionRate: () => api.get('/stats/resolution-rate'),
};

// Categories API
export const categoryAPI = {
  getAll: () => api.get('/categories'),
  
  getById: (id: number) => api.get(`/categories/${id}`),
};

// Sectors API
export const sectorAPI = {
  getAll: () => api.get('/sectors'),
  
  getById: (id: number) => api.get(`/sectors/${id}`),
  
  getIncidents: (id: number) => api.get(`/sectors/${id}/incidents`),
};

// Admin incidents API (admin actions)
export const incidentsApi = {
  changeStatus: async (id: number, newStatus: string) => {
    const response = await api.patch(`/incidents/${id}/status`, { newStatus });
    return response.data;
  },

  delete: async (id: number) => {
    await api.delete(`/incidents/${id}`);
  },

  getAll: (params?: any) => api.get('/incidents', { params }),
};

// Users API (Admin only)
export const userAPI = {
  getAll: (params?: { page?: number; size?: number }) => api.get('/users', { params }),
  
  getById: (id: number) => api.get(`/users/${id}`),
  
  deactivate: (id: number) => api.delete(`/users/${id}`),
  
  getStats: () => api.get('/users/stats'),
};

// Admin Users API
export const adminUsersApi = {
  getAll: async () => {
    const response = await api.get('/admin/users');
    return response.data;
  },

  create: async (userData: any) => {
    const response = await api.post('/admin/users', userData);
    return response.data;
  },

  update: async (id: number, userData: any) => {
    const response = await api.put(`/admin/users/${id}`, userData);
    return response.data;
  },

  delete: async (id: number) => {
    await api.delete(`/admin/users/${id}`);
  },
};

export default api;
