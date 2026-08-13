import request from '@/utils/request'

/**
 * 上传图片到 MinIO 对象存储
 * @param {File} file 图片文件
 * @returns {Promise<string>} 图片可访问 URL
 */
export const uploadImage = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/admin/upload/image', fd)
}
