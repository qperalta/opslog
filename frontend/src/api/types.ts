// mirrors the Java response records

export interface User {
  id: number
  email: string
  displayName: string
}

export interface LoginResponse {
  token: string
  expiresAt: string
  user: User
}

export interface Lookup {
  id: number
  name: string
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
