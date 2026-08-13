import request from '@/utils/request'

export const login = (phone, password) =>
    request.post('/user/login', { phone, password })

export const register = (data) =>
    request.post('/user/register', data)

export const sendResetCode = (phone) =>
    request.post('/user/send-code', { phone })

export const forgotPassword = (phone, code) =>
    request.post('/user/forgot-password', { phone, code })