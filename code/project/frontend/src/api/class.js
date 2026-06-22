import request from '@/utils/request'

export function getClassList(params) {
  return request({
    url: '/class/list',
    method: 'get',
    params
  })
}

export function getClassDetail(id) {
  return request({
    url: `/class/${id}`,
    method: 'get'
  })
}

export function createClass(data) {
  return request({
    url: '/class/add',
    method: 'post',
    data
  })
}

export function updateClass(id, data) {
  return request({
    url: `/class/update/${id}`,
    method: 'put',
    data
  })
}

export function deleteClass(id) {
  return request({
    url: `/class/delete/${id}`,
    method: 'delete'
  })
}

export function addClassMembers(classId, data) {
  return request({
    url: `/class/member/add`,
    method: 'post',
    params: { classId },
    data
  })
}

export function removeClassMembers(classId, data) {
  return request({
    url: `/class/member/remove`,
    method: 'delete',
    params: { classId },
    data
  })
}

export function getClassMembers(classId) {
  return request({
    url: `/class/${classId}/members`,
    method: 'get'
  })
}

export function getMyClasses() {
  return request({
    url: '/class/my-classes',
    method: 'get'
  })
}
