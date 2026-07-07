// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 查询所有应用代码版本 GET /appVersion/list */
export async function listAppVersions(options?: { [key: string]: any }) {
  return request<API.AppVersion[]>('/appVersion/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 根据主键获取应用代码版本 GET /appVersion/getInfo/${param0} */
export async function getAppVersionInfo(
  params: API.getAppVersionInfoParams,
  options?: { [key: string]: any },
) {
  const { id: param0, ...queryParams } = params
  return request<API.AppVersion>(`/appVersion/getInfo/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}
