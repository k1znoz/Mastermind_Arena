<script>
  export let src
  export let size = 24
</script>

<span aria-hidden="true" style:--icon-url={`url("${src}")`} style:width={`${size}px`} style:height={`${size}px`}></span>

<style>
  span { display:inline-block; flex:none; vertical-align:middle; background:currentColor; mask:var(--icon-url) center / contain no-repeat; }
</style>
