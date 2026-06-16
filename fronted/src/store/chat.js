import { defineStore } from 'pinia'

export const useChatStore = defineStore('chat', {
  state: () => ({
    isChatOpen: false,
    activeContactId: null,
    activeContactName: '',
    activeGoodsId: null
  }),
  actions: {
    openChat(contactId, contactName, goodsId = null) {
      this.activeContactId = contactId
      this.activeContactName = contactName
      this.activeGoodsId = goodsId
      this.isChatOpen = true
    },
    closeChat() {
      this.isChatOpen = false
      this.activeContactId = null
      this.activeContactName = ''
      this.activeGoodsId = null
    }
  }
})
