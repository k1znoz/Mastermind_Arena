import { mount } from 'svelte'
import './app.css'
import PublicInformation from './lib/components/PublicInformation.svelte'

const target = document.getElementById('app')
mount(PublicInformation, {
  target,
  props: { page: target.dataset.informationPage }
})
