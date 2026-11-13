// mirrors NotificationsResponse and NotificationResponse
export interface Notification {
  id: number
  title: string
  body: string
  publishedAt: string
  unread: boolean
}

export interface Notifications {
  unreadCount: number
  items: Notification[]
}
