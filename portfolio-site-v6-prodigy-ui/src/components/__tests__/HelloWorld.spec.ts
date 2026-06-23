import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import HelloWorld from '../HelloWorld.vue'

describe('HelloWorld', () => {
  it('renders hello', () => {
    const wrapper = mount(HelloWorld)
    expect(wrapper.text()).toBe('Hello')
  })
})
