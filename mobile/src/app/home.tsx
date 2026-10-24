import { useCallback, useEffect, useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Pressable,
    RefreshControl,
    ScrollView,
    StatusBar,
    StyleSheet,
    Text,
    View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import { API_BASE_URL } from '@/constants/api';

type AssignmentStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'REVOKED';

type Assignment = {
    assignmentId: string;
    eventId: string;
    eventName: string;
    eventDate: string | null;
    eventTime: string | null;
    eventLocation: string | null;
    userId: string;
    name: string;
    email: string;
    status: AssignmentStatus;
};

export default function HomeScreen() {
    const [assignments, setAssignments] = useState<Assignment[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [actioningId, setActioningId] = useState<string | null>(null);

    const loadAssignments = useCallback(async (isRefresh = false) => {
        if (isRefresh) {
            setRefreshing(true);
        } else {
            setLoading(true);
        }

        try {
            const token = await SecureStore.getItemAsync('entrypulse_token');

            if (!token) {
                setAssignments([]);
                setError('Your session has expired. Please sign in again.');
                return;
            }

            const response = await fetch(
                `${API_BASE_URL}/api/event-security/my-assignments`,
                {
                    method: 'GET',
                    headers: {
                        Authorization: `Bearer ${token}`,
                        Accept: 'application/json',
                    },
                }
            );

            if (response.status === 401) {
                await SecureStore.deleteItemAsync('entrypulse_token');
                setAssignments([]);
                router.replace('/');
                return;
            }

            if (!response.ok) {
                throw new Error(
                    `Could not load assignments (HTTP ${response.status}).`
                );
            }

            const data: Assignment[] = await response.json();

            if (!Array.isArray(data)) {
                throw new Error('The server returned an unexpected response.');
            }

            setAssignments(data);
            setError(null);
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : 'Unable to connect to the server.'
            );
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    }, []);

    useEffect(() => {
        void loadAssignments();
    }, [loadAssignments]);

    async function respondToAssignment(
        assignment: Assignment,
        action: 'accept' | 'decline'
    ) {
        const token = await SecureStore.getItemAsync('entrypulse_token');

        if (!token) {
            Alert.alert('Session expired', 'Please sign in again.');
            router.replace('/');
            return;
        }

        setActioningId(assignment.assignmentId);

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/event-security/${assignment.assignmentId}/${action}`,
                {
                    method: 'POST',
                    headers: {
                        Authorization: `Bearer ${token}`,
                        Accept: 'application/json',
                    },
                }
            );

            const data = await response.json().catch(() => null);

            if (response.status === 401) {
                await SecureStore.deleteItemAsync('entrypulse_token');
                router.replace('/');
                return;
            }

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    `Unable to ${action} this assignment.`
                );
            }

            await loadAssignments(true);

            Alert.alert(
                action === 'accept' ? 'Assignment accepted' : 'Assignment declined',
                action === 'accept'
                    ? 'This event is now available under My Events.'
                    : 'Your response has been recorded.'
            );
        } catch (err) {
            Alert.alert(
                'Request failed',
                err instanceof Error
                    ? err.message
                    : 'Unable to connect to the server.'
            );
        } finally {
            setActioningId(null);
        }
    }

    function confirmResponse(
        assignment: Assignment,
        action: 'accept' | 'decline'
    ) {
        if (action === 'accept') {
            void respondToAssignment(assignment, action);
            return;
        }

        Alert.alert(
            'Decline assignment',
            `Are you sure you want to decline "${assignment.eventName}"?`,
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Decline',
                    style: 'destructive',
                    onPress: () => void respondToAssignment(assignment, action),
                },
            ]
        );
    }

    function handleLogout() {
        Alert.alert('Log out', 'Are you sure you want to log out?', [
            { text: 'Cancel', style: 'cancel' },
            {
                text: 'Log out',
                style: 'destructive',
                onPress: async () => {
                    await SecureStore.deleteItemAsync('entrypulse_token');
                    router.replace('/');
                },
            },
        ]);
    }

    function formatDate(date: string | null) {
        if (!date) return 'Date to be confirmed';

        const parsed = new Date(`${date}T00:00:00`);

        if (Number.isNaN(parsed.getTime())) return date;

        return parsed.toLocaleDateString(undefined, {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
        });
    }

    function formatTime(time: string | null) {
        if (!time) return 'Time to be confirmed';

        return time.slice(0, 5);
    }

    const pendingAssignments = assignments.filter(
        (assignment) => assignment.status === 'PENDING'
    );

    const acceptedAssignments = assignments.filter(
        (assignment) => assignment.status === 'ACCEPTED'
    );

    function renderAssignmentCard(
        assignment: Assignment,
        isPending: boolean
    ) {
        const isActioning = actioningId === assignment.assignmentId;

        return (
            <View key={assignment.assignmentId} style={styles.eventCard}>
                <View style={styles.eventInfo}>
                    <View style={styles.eventTitleRow}>
                        <Text style={styles.eventName}>
                            {assignment.eventName || 'Untitled event'}
                        </Text>

                        <View
                            style={[
                                styles.badge,
                                isPending ? styles.pendingBadge : styles.acceptedBadge,
                            ]}
                        >
                            <Text
                                style={[
                                    styles.badgeText,
                                    isPending ? styles.pendingText : styles.acceptedText,
                                ]}
                            >
                                {isPending ? 'Pending' : 'Accepted'}
                            </Text>
                        </View>
                    </View>

                    <Text style={styles.eventDetails}>
                        {formatDate(assignment.eventDate)}
                    </Text>

                    <Text style={styles.eventDetails}>
                        {formatTime(assignment.eventTime)}
                    </Text>

                    <Text style={styles.eventDetails}>
                        {assignment.eventLocation || 'Venue to be confirmed'}
                    </Text>
                </View>

                {isPending ? (
                    <View style={styles.actionRow}>
                        <Pressable
                            style={({ pressed }) => [
                                styles.actionButton,
                                styles.acceptButton,
                                (pressed || isActioning) && styles.buttonPressed,
                            ]}
                            disabled={isActioning}
                            onPress={() => confirmResponse(assignment, 'accept')}
                        >
                            <Text style={styles.actionButtonText}>
                                {isActioning ? 'Please wait...' : 'Accept'}
                            </Text>
                        </Pressable>

                        <Pressable
                            style={({ pressed }) => [
                                styles.actionButton,
                                styles.declineButton,
                                (pressed || isActioning) && styles.buttonPressed,
                            ]}
                            disabled={isActioning}
                            onPress={() => confirmResponse(assignment, 'decline')}
                        >
                            <Text style={styles.declineButtonText}>Decline</Text>
                        </Pressable>
                    </View>
                ) : (
                    <Pressable
                        style={({ pressed }) => [
                            styles.openButton,
                            pressed && styles.buttonPressed,
                        ]}
                        onPress={() =>
                            router.push({
                                pathname: '/event',
                                params: {
                                    eventId: assignment.eventId,
                                    eventName: assignment.eventName,
                                    eventDate: assignment.eventDate ?? '',
                                    eventTime: assignment.eventTime ?? '',
                                    eventLocation: assignment.eventLocation ?? '',
                                },
                            })
                        }
                    >
                        <Text style={styles.openButtonText}>Open event</Text>
                    </Pressable>
                )}
            </View>
        );
    }

    return (
        <SafeAreaView style={styles.safeArea} edges={['top', 'bottom']}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            <View style={styles.header}>
                <Text style={styles.headerTitle}>My events</Text>
                <Text style={styles.headerSubtitle}>EntryPulse Security</Text>
            </View>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.eventList}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={() => void loadAssignments(true)}
                        tintColor="#117A91"
                    />
                }
            >
                {loading ? (
                    <View style={styles.centerState}>
                        <ActivityIndicator size="large" color="#117A91" />
                        <Text style={styles.stateDescription}>
                            Loading your assignments...
                        </Text>
                    </View>
                ) : error ? (
                    <View style={styles.centerState}>
                        <Text style={styles.emptyTitle}>Unable to load assignments</Text>
                        <Text style={styles.stateDescription}>{error}</Text>

                        <Pressable
                            style={styles.retryButton}
                            onPress={() => void loadAssignments()}
                        >
                            <Text style={styles.openButtonText}>Try again</Text>
                        </Pressable>

                        {!assignments.length && error.includes('session') && (
                            <Pressable
                                style={styles.loginButton}
                                onPress={() => router.replace('/')}
                            >
                                <Text style={styles.loginButtonText}>Sign in again</Text>
                            </Pressable>
                        )}
                    </View>
                ) : (
                    <>
                        <View style={styles.sectionHeader}>
                            <Text style={styles.sectionTitle}>Pending requests</Text>
                            <View style={styles.countBadge}>
                                <Text style={styles.countText}>
                                    {pendingAssignments.length}
                                </Text>
                            </View>
                        </View>

                        {pendingAssignments.length > 0 ? (
                            pendingAssignments.map((assignment) =>
                                renderAssignmentCard(assignment, true)
                            )
                        ) : (
                            <Text style={styles.sectionEmpty}>
                                You have no pending assignment requests.
                            </Text>
                        )}

                        <View style={styles.sectionHeader}>
                            <Text style={styles.sectionTitle}>My Events</Text>
                            <View style={styles.countBadge}>
                                <Text style={styles.countText}>
                                    {acceptedAssignments.length}
                                </Text>
                            </View>
                        </View>

                        {acceptedAssignments.length > 0 ? (
                            acceptedAssignments.map((assignment) =>
                                renderAssignmentCard(assignment, false)
                            )
                        ) : (
                            <Text style={styles.sectionEmpty}>
                                Accepted assignments will appear here.
                            </Text>
                        )}
                    </>
                )}

                <Text style={styles.refreshHint}>
                    Pull down to refresh your assignments.
                </Text>
            </ScrollView>

            <View style={styles.footer}>
                <Pressable
                    style={({ pressed }) => [
                        styles.logoutButton,
                        pressed && styles.logoutPressed,
                    ]}
                    onPress={handleLogout}
                >
                    <Text style={styles.logoutText}>Log out</Text>
                </Pressable>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F4F6F7',
    },
    header: {
        minHeight: 76,
        justifyContent: 'center',
        paddingHorizontal: 20,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#DFE5E9',
    },
    headerTitle: {
        fontSize: 21,
        fontWeight: '800',
        color: '#1B2935',
    },
    headerSubtitle: {
        marginTop: 3,
        fontSize: 12,
        color: '#657585',
    },
    scrollView: {
        flex: 1,
    },
    eventList: {
        paddingHorizontal: 18,
        paddingTop: 18,
        paddingBottom: 20,
        flexGrow: 1,
    },
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 9,
        marginBottom: 13,
        marginTop: 8,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '800',
        color: '#1B2935',
    },
    countBadge: {
        minWidth: 26,
        height: 26,
        borderRadius: 13,
        backgroundColor: '#DCEEF2',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 7,
    },
    countText: {
        fontSize: 12,
        fontWeight: '800',
        color: '#117A91',
    },
    eventCard: {
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#DEE5E9',
        borderRadius: 15,
        padding: 18,
        marginBottom: 15,
    },
    eventInfo: {
        marginBottom: 13,
    },
    eventTitleRow: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 8,
        marginBottom: 5,
    },
    eventName: {
        flex: 1,
        fontSize: 18,
        lineHeight: 25,
        fontWeight: '800',
        color: '#1B2935',
    },
    badge: {
        borderRadius: 20,
        paddingHorizontal: 10,
        paddingVertical: 5,
    },
    pendingBadge: {
        backgroundColor: '#FFF0D5',
    },
    acceptedBadge: {
        backgroundColor: '#DDF3E5',
    },
    badgeText: {
        fontSize: 12,
        fontWeight: '700',
    },
    pendingText: {
        color: '#8A5700',
    },
    acceptedText: {
        color: '#16713C',
    },
    eventDetails: {
        marginTop: 5,
        fontSize: 14,
        lineHeight: 20,
        color: '#657585',
    },
    actionRow: {
        flexDirection: 'row',
        gap: 10,
    },
    actionButton: {
        flex: 1,
        minHeight: 50,
        borderRadius: 12,
        alignItems: 'center',
        justifyContent: 'center',
    },
    acceptButton: {
        backgroundColor: '#117A91',
    },
    declineButton: {
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#D5DEE3',
    },
    actionButtonText: {
        fontSize: 15,
        fontWeight: '800',
        color: '#FFFFFF',
    },
    declineButtonText: {
        fontSize: 15,
        fontWeight: '800',
        color: '#A52D36',
    },
    openButton: {
        minHeight: 50,
        borderRadius: 12,
        backgroundColor: '#117A91',
        alignItems: 'center',
        justifyContent: 'center',
    },
    openButtonText: {
        fontSize: 15,
        fontWeight: '800',
        color: '#FFFFFF',
    },
    buttonPressed: {
        opacity: 0.75,
    },
    sectionEmpty: {
        fontSize: 14,
        lineHeight: 21,
        color: '#657585',
        marginBottom: 22,
        paddingVertical: 8,
    },
    centerState: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 45,
        paddingHorizontal: 15,
    },
    emptyTitle: {
        fontSize: 18,
        fontWeight: '800',
        color: '#1B2935',
        textAlign: 'center',
    },
    stateDescription: {
        marginTop: 10,
        marginBottom: 15,
        fontSize: 14,
        lineHeight: 21,
        color: '#657585',
        textAlign: 'center',
    },
    retryButton: {
        minHeight: 48,
        minWidth: 140,
        borderRadius: 12,
        backgroundColor: '#117A91',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 18,
    },
    loginButton: {
        padding: 12,
        marginTop: 8,
    },
    loginButtonText: {
        color: '#117A91',
        fontWeight: '800',
    },
    refreshHint: {
        marginTop: 12,
        marginBottom: 15,
        textAlign: 'center',
        fontSize: 13,
        color: '#657585',
    },
    footer: {
        paddingHorizontal: 18,
        paddingTop: 13,
        paddingBottom: 12,
        backgroundColor: '#FFFFFF',
        borderTopWidth: 1,
        borderTopColor: '#DFE5E9',
    },
    logoutButton: {
        minHeight: 55,
        borderRadius: 13,
        borderWidth: 1,
        borderColor: '#CBD5DC',
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center',
    },
    logoutPressed: {
        backgroundColor: '#F0F3F5',
    },
    logoutText: {
        fontSize: 16,
        fontWeight: '800',
        color: '#1B2935',
    },
});