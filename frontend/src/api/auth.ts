import request from './request'

export interface SlideCaptchaResult {
  captchaKey: string
  bgImage: string
  pieceImage: string
  pieceY: number
  pieceSize: number
}

export interface LoginResult {
  token: string
  username: string
  name: string
}

/** 获取滑块验证码 */
export function getSlideCaptcha() {
  return request.get('/auth/slide-captcha') as unknown as Promise<SlideCaptchaResult>
}

/** 验证滑块位置 */
export function verifySlideCaptcha(captchaKey: string, positionX: number) {
  return request.post('/auth/slide-captcha/verify', { captchaKey, positionX }) as unknown as Promise<{ captchaToken: string }>
}

/** 用户登录 */
export function login(data: {
  username: string
  password: string
  captchaToken: string
}) {
  return request.post('/auth/login', data) as unknown as Promise<LoginResult>
}

/** 用户注册 */
export function register(data: {
  username: string
  password: string
  captchaToken: string
}) {
  return request.post('/auth/register', data) as unknown as Promise<LoginResult>
}
