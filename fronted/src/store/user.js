import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || null,
    username: localStorage.getItem('username') || '',
    nickname: localStorage.getItem('nickname') || '',
    role: localStorage.getItem('role') || ''
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  actions: {
    login(userData) {
      this.token = userData.token
      this.userId = userData.userId
      this.username = userData.username
      this.nickname = userData.nickname
      this.role = userData.role

      localStorage.setItem('token', userData.token)
      localStorage.setItem('userId', String(userData.userId))
      localStorage.setItem('username', userData.username)
      localStorage.setItem('nickname', userData.nickname)
      localStorage.setItem('role', userData.role)
    },
    logout() {
      this.token = ''
      this.userId = null
      this.username = ''
      this.nickname = ''
      this.role = ''

      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('nickname')
      localStorage.removeItem('role')
    }
  }
})
