const requestUse = jest.fn()
const responseUse = jest.fn()
const mockGet = jest.fn(() => Promise.resolve({ data: {} }))
const mockPost = jest.fn(() => Promise.resolve({ data: {} }))
const mockPatch = jest.fn(() => Promise.resolve({ data: {} }))
const mockPut = jest.fn(() => Promise.resolve({ data: {} }))
const mockDelete = jest.fn(() => Promise.resolve({ data: {} }))

jest.mock('axios', () => ({
  create: () => ({
    defaults: { baseURL: 'http://api.test/api/v1' },
    interceptors: {
      request: {
        use: requestUse,
      },
      response: {
        use: responseUse,
      },
    },
    get: mockGet,
    post: mockPost,
    patch: mockPatch,
    put: mockPut,
    delete: mockDelete,
  }),
}))

describe('api client', () => {
  beforeEach(() => {
    jest.clearAllMocks()
    jest.resetModules()
    localStorage.clear()
  })

  it('registers request and response interceptors', async () => {
    await import('@/lib/api')

    expect(requestUse).toHaveBeenCalledTimes(1)
    expect(responseUse).toHaveBeenCalledTimes(1)
  })

  it('request interceptor attaches bearer token when present', async () => {
    await import('@/lib/api')
    localStorage.setItem('urbanops_token', 'abc')
    const onRequest = requestUse.mock.calls[0][0]

    const config = onRequest({ headers: {} })

    expect(config.headers.Authorization).toBe('Bearer abc')
  })

  it('response interceptor clears auth and redirects on non-auth 401', async () => {
    await import('@/lib/api')
    localStorage.setItem('urbanops_token', 'abc')
    localStorage.setItem('urbanops_user', '{}')
    const onError = responseUse.mock.calls[0][1]

    await expect(onError({ response: { status: 401 }, config: { url: '/incidents' } })).rejects.toBeTruthy()

    expect(localStorage.getItem('urbanops_token')).toBeNull()
    expect(localStorage.getItem('urbanops_user')).toBeNull()
  })

  it('does not redirect for auth 401 attempts', async () => {
    await import('@/lib/api')
    localStorage.setItem('urbanops_token', 'abc')
    const onError = responseUse.mock.calls[0][1]

    await expect(onError({ response: { status: 401 }, config: { url: '/auth/login' } })).rejects.toBeTruthy()

    expect(localStorage.getItem('urbanops_token')).toBe('abc')
  })

  it('calls auth, stats, category, sector, alert, user and admin endpoints', async () => {
    const {
      adminUsersApi,
      alertAPI,
      authAPI,
      categoryAPI,
      incidentsApi,
      sectorAPI,
      statsAPI,
      userAPI,
    } = await import('@/lib/api')
    mockGet.mockResolvedValue({ data: ['ok'] })
    mockPost.mockResolvedValue({ data: { id: 1 } })
    mockPatch.mockResolvedValue({ data: { status: 'OPEN' } })
    mockDelete.mockResolvedValue({})

    await authAPI.login('a@test.ma', 'secret')
    await authAPI.register({ firstName: 'A', lastName: 'B', email: 'a@test.ma', password: 'secret' })
    await authAPI.getMe()
    await statsAPI.getDashboard()
    await statsAPI.getByCategory()
    await statsAPI.getBySector()
    await statsAPI.getHourly()
    await statsAPI.getServicesHealth()
    await statsAPI.getResolutionRate()
    await categoryAPI.getAll()
    await categoryAPI.getById(1)
    await sectorAPI.getAll()
    await sectorAPI.getById(1)
    await sectorAPI.getIncidents(1)
    await alertAPI.getAll({ acknowledged: false })
    await alertAPI.getById(1)
    await alertAPI.getByIncident(2)
    await alertAPI.resend(3)
    await alertAPI.acknowledge(4)
    await alertAPI.getRecent()
    await alertAPI.getCritical()
    await userAPI.getAll({ page: 0 })
    await userAPI.getById(1)
    await userAPI.deactivate(1)
    await userAPI.getStats()
    await adminUsersApi.getAll()
    await adminUsersApi.create({ email: 'x@test.ma' })
    await adminUsersApi.update(1, { firstName: 'X' })
    await adminUsersApi.delete(1)
    await incidentsApi.changeStatus(1, 'RESOLVED')
    await incidentsApi.delete(1)
    await incidentsApi.getAll({ page: 0 })

    expect(mockPost).toHaveBeenCalledWith('/auth/login', { email: 'a@test.ma', password: 'secret' })
    expect(mockPatch).toHaveBeenCalledWith('/incidents/1/status', { newStatus: 'RESOLVED' })
    expect(mockDelete).toHaveBeenCalledWith('/admin/users/1')
  })

  it('logout clears local storage before posting logout', async () => {
    const { authAPI } = await import('@/lib/api')
    localStorage.setItem('urbanops_token', 'abc')
    localStorage.setItem('urbanops_user', '{}')
    mockPost.mockResolvedValue({ data: {} })

    await authAPI.logout()

    expect(localStorage.getItem('urbanops_token')).toBeNull()
    expect(localStorage.getItem('urbanops_user')).toBeNull()
    expect(mockPost).toHaveBeenCalledWith('/auth/logout')
  })

  it('incident create uses fetch with auth header and parses success', async () => {
    const { incidentAPI } = await import('@/lib/api')
    localStorage.setItem('urbanops_token', 'abc')
    const formData = new FormData()
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: jest.fn().mockResolvedValue({ id: 1 }),
    }) as any

    await expect(incidentAPI.create(formData)).resolves.toEqual({ data: { id: 1 } })

    expect(global.fetch).toHaveBeenCalledWith('http://api.test/api/v1/incidents', {
      method: 'POST',
      headers: { Authorization: 'Bearer abc' },
      body: formData,
    })
  })

  it('incident create throws backend error payload when fetch fails', async () => {
    const { incidentAPI } = await import('@/lib/api')
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      json: jest.fn().mockResolvedValue({ message: 'bad' }),
    }) as any

    await expect(incidentAPI.create(new FormData())).rejects.toEqual({
      response: { data: { message: 'bad' } },
    })
  })
})
