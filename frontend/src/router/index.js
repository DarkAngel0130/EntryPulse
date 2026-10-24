import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../auth/session'
import RegisterView from '../views/auth/RegisterView.vue'

const routes = [
    { path: '/login', component: () => import('../views/auth/LoginView.vue') },

    {
        path: '/register',
        name: 'register',
        component: RegisterView,
        meta: { guestOnly: true }
    },
    { path: '/', redirect: () => (isAuthenticated() ? '/organiser' : '/login') },

    {
        path: '/organiser',
        component: () => import('../layouts/OrganiserLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            { path: '', component: () => import('../views/organiser/OrganiserHome.vue') },
            { path: 'events', component: () => import('../views/organiser/EventsView.vue') },
            {
                path: 'events/:eventId',
                component: () => import('../layouts/EventLayout.vue'),
                children: [
                    { path: '', component: () => import('../views/organiser/EventOverview.vue') },
                    { path: 'guests', component: () => import('../views/organiser/GuestsView.vue') },
                    { path: 'seating', component: () => import('../views/organiser/SeatingView.vue') },
                    { path: 'invitations', component: () => import('../views/organiser/InvitationsView.vue') },
                    { path: 'security', component: () => import('../views/organiser/SecurityView.vue') },
                    { path: 'attendance', component: () => import('../views/organiser/AttendanceView.vue') },

                ],
            },
        ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
    if (to.meta.requiresAuth && !isAuthenticated()) {
        return '/login'
    }

    if (
        (to.path === '/login' || to.path === '/register') &&
        isAuthenticated()
    ) {
        return '/organiser'
    }
})


export default router