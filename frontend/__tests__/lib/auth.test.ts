import {
  canAccessAdminDashboard,
  getCurrentRole,
  getDashboardPath,
  getStoredUser,
  getTokenPayload,
  isAdminFromToken,
  isAdminUser,
  normalizeRole,
} from '@/lib/auth'

function tokenWithPayload(payload: Record<string, unknown>) {
  return `header.${btoa(JSON.stringify(payload))}.signature`
}

describe('auth helpers', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('normalizes roles defensively', () => {
    expect(normalizeRole('ROLE_ADMIN')).toBe('ADMIN')
    expect(normalizeRole('manager')).toBe('MANAGER')
    expect(normalizeRole(null)).toBe('')
    expect(normalizeRole('')).toBe('')
  })

  it('returns null when token or stored user are missing or invalid', () => {
    expect(getTokenPayload()).toBeNull()
    expect(getStoredUser()).toBeNull()

    localStorage.setItem('urbanops_token', 'bad-token')
    localStorage.setItem('urbanops_user', '{bad-json')

    expect(getTokenPayload()).toBeNull()
    expect(getStoredUser()).toBeNull()
  })

  it('reads token payload and stored user from localStorage', () => {
    localStorage.setItem('urbanops_token', tokenWithPayload({ role: 'ROLE_MANAGER', firstName: 'Mina' }))
    localStorage.setItem('urbanops_user', JSON.stringify({ role: 'ADMIN', firstName: 'Yassine' }))

    expect(getTokenPayload()).toMatchObject({ role: 'ROLE_MANAGER', firstName: 'Mina' })
    expect(getStoredUser()).toMatchObject({ role: 'ADMIN', firstName: 'Yassine' })
  })

  it('prefers stored user role and computes dashboard access', () => {
    localStorage.setItem('urbanops_token', tokenWithPayload({ role: 'ROLE_CITIZEN' }))
    localStorage.setItem('urbanops_user', JSON.stringify({ role: 'manager' }))

    expect(getCurrentRole()).toBe('MANAGER')
    expect(canAccessAdminDashboard()).toBe(true)
    expect(isAdminUser()).toBe(false)
    expect(isAdminFromToken()).toBe(true)
    expect(getDashboardPath()).toBe('/dashboard')
  })

  it('falls back to citizen route when no admin role is present', () => {
    localStorage.setItem('urbanops_token', tokenWithPayload({ role: 'ROLE_CITIZEN' }))

    expect(getCurrentRole()).toBe('CITIZEN')
    expect(canAccessAdminDashboard()).toBe(false)
    expect(isAdminUser()).toBe(false)
    expect(getDashboardPath()).toBe('/mes-signalements')
  })
})
