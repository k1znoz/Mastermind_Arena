import js from '@eslint/js'
import globals from 'globals'
import svelte from 'eslint-plugin-svelte'

export default [
  {
    ignores: ['dist/**', 'node_modules/**']
  },
  js.configs.recommended,
  ...svelte.configs['flat/recommended'],
  {
    files: ['src/**/*.{js,svelte}', '*.js'],
    languageOptions: {
      globals: {
        ...globals.browser,
        ...globals.node
      }
    },
    rules: {
      'no-unused-vars': ['error', {
        argsIgnorePattern: '^_',
        caughtErrorsIgnorePattern: '^_'
      }],
      'svelte/require-each-key': 'off'
    }
  }
]
