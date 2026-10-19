import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../auth/session'

const routes = [
    { path: '/login', component: () => import('../views/auth/LoginView.vue') },
    { path: '/', redirect: () => (isAuthenticated() ? '/organiser' : '/login') },
    {
        path: '/organiser',
        component: () => import('../layouts/OrganiserLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            { path: '', component: () => import('../views/organiser/OrganiserHome.vue') },
            { path: 'events', component: () => import('../views/organiser/EventsView.vue') },
        ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
    if (to.meta.requiresAuth && !isAuthenticated()) return '/login'
    if (to.path === '/login' && isAuthenticated()) return '/organiser'
})

export default router