import { useCallback, useState } from 'react';
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
import {
    router,
    useFocusEffect,
    useLocalSearchParams,
} from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import { API_BASE_URL } from '@/constants/api';

type CheckInActivity = {
    id?: string;
    guestName?: string;
    name?: string;
    checkInTime?: string;
    checkedInAt?: string;
};

export default function EventScreen() {
    const {
        eventId,
        eventName,
        eventDate,
        eventTime,
        eventLocation,
    } = useLocalSearchParams<{
        eventId: string;
        eventName: string;
        eventDate?: string;
        eventTime?: string;
        eventLocation?: string;
    }>();

    const [checkInCount, setCheckInCount] = useState<number | null>(null);
    const [activity, setActivity] = useState<CheckInActivity[]>([]);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [showActivity, setShowActivity] = useState(false);

    const loadDashboard = useCallback(async (refresh = false) => {
        if (!eventId) {
            setError('The event ID is missing. Please return to My Events.');
            setLoading(false);
            return;
        }

        if (refresh) {
            setRefreshing(true);
        } else {
            setLoading(true);
        }

        try {
            const token = await SecureStore.getItemAsync('entrypulse_token');

            if (!token) {
                router.replace('/');
                return;
            }

            const headers = {
                Authorization: `Bearer ${token}`,
                Accept: 'application/json',
            };

            const [countResponse, activityResponse] = await Promise.all([
                fetch(
                    `${API_BASE_URL}/api/check-ins/event/${encodeURIComponent(eventId)}/my-count`,
                    { headers }
                ),
                fetch(
                    `${API_BASE_URL}/api/check-ins/event/${encodeURIComponent(eventId)}/my-activity`,
                    { headers }
                ),
            ]);

            if (
                countResponse.status === 401 ||
                activityResponse.status === 401
            ) {
                await SecureStore.deleteItemAsync('entrypulse_token');
                router.replace('/');
                return;
            }

            if (!countResponse.ok) {
                throw new Error(
                    `Unable to load check-in count (HTTP ${countResponse.status}).`
                );
            }

            const countData: unknown = await countResponse.json();

            if (
                typeof countData !== 'number' ||
                !Number.isInteger(countData) ||
                countData < 0
            ) {
                throw new Error('The server returned an invalid check-in count.');
            }

            setCheckInCount(countData);

            if (activityResponse.ok) {
                const activityData: unknown = await activityResponse.json();

                if (Array.isArray(activityData)) {
                    setActivity(activityData);
                }
            }

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
    }, [eventId]);

    useFocusEffect(
        useCallback(() => {
            void loadDashboard();
        }, [loadDashboard])
    );

    function formatDate(date?: string) {
        if (!date) return 'Date to be confirmed';

        const parsed = new Date(`${date}T00:00:00`);

        if (Number.isNaN(parsed.getTime())) return date;

        return parsed.toLocaleDateString(undefined, {
            weekday: 'short',
            day: 'numeric',
            month: 'short',
        });
    }

    function formatTime(time?: string) {
        return time ? time.slice(0, 5) : 'Time to be confirmed';
    }

    function openScanner() {
        router.push({
            pathname: '/scan',
            params: {
                eventId: String(eventId),
                eventName: String(eventName ?? 'Event'),
            },
        });
    }


    function openGuestSearch() {
        router.push({
            pathname: '/scan',
            params: {
                eventId: String(eventId),
                eventName: String(eventName ?? 'Event'),
                searchMode: 'true',
            },
        });
    }


    function openRecentActivity() {
        setShowActivity(true);
    }

    return (
        <SafeAreaView style={styles.safeArea} edges={['top', 'bottom']}>
            <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

            <View style={styles.header}>
                <Pressable
                    onPress={() => router.back()}
                    hitSlop={12}
                    style={styles.backButton}
                    accessibilityRole="button"
                    accessibilityLabel="Go back"
                >
                    <Text style={styles.backIcon}>‹</Text>
                </Pressable>

                <Text style={styles.headerTitle}>Event</Text>
            </View>

            <ScrollView
                style={styles.scrollView}
                contentContainerStyle={styles.content}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={() => void loadDashboard(true)}
                        tintColor="#117A91"
                    />
                }
            >
                <View style={styles.eventCard}>
                    <Text style={styles.eventName}>
                        {eventName || 'Untitled event'}
                    </Text>

                    <Text style={styles.eventSubtitle}>
                        {formatDate(eventDate)} · {eventLocation || 'Venue to be confirmed'}
                    </Text>

                    {eventTime ? (
                        <Text style={styles.eventTime}>
                            {formatTime(eventTime)}
                        </Text>
                    ) : null}

                    <View style={styles.countRow}>
                        {loading && checkInCount === null ? (
                            <ActivityIndicator color="#117A91" />
                        ) : (
                            <Text style={styles.countDescription}>
                                You have checked in{' '}
                                <Text style={styles.countNumber}>
                                    {checkInCount === null ? '—' : checkInCount}
                                </Text>{' '}
                                {checkInCount === 1 ? 'guest' : 'guests'}
                            </Text>
                        )}
                    </View>

                    {error ? (
                        <View style={styles.errorContainer}>
                            <Text style={styles.errorText}>{error}</Text>
                            <Pressable
                                onPress={() => void loadDashboard(true)}
                            >
                                <Text style={styles.retryText}>Try again</Text>
                            </Pressable>
                        </View>
                    ) : null}
                </View>

                <Pressable
                    style={({ pressed }) => [
                        styles.primaryButton,
                        pressed && styles.buttonPressed,
                    ]}
                    onPress={openScanner}
                >
                    <Text style={styles.primaryButtonText}>
                        Scan invitation
                    </Text>
                </Pressable>

                <Pressable
                    style={({ pressed }) => [
                        styles.secondaryButton,
                        pressed && styles.buttonPressed,
                    ]}
                    onPress={openGuestSearch}
                >
                    <Text style={styles.secondaryButtonText}>
                        Find a guest by name
                    </Text>
                </Pressable>

                <Pressable
                    style={({ pressed }) => [
                        styles.secondaryButton,
                        pressed && styles.buttonPressed,
                    ]}
                    onPress={openRecentActivity}
                >
                    <Text style={styles.secondaryButtonText}>
                        My recent activity
                    </Text>
                </Pressable>
            </ScrollView>

            {showActivity ? (
                <View style={styles.modalOverlay}>
                    <View style={styles.activityPanel}>
                        <View style={styles.activityHeader}>
                            <Text style={styles.activityTitle}>
                                My recent activity
                            </Text>
                            <Pressable onPress={() => setShowActivity(false)}>
                                <Text style={styles.closeButton}>Close</Text>
                            </Pressable>
                        </View>

                        {activity.length === 0 ? (
                            <Text style={styles.emptyActivity}>
                                No check-ins to display yet.
                            </Text>
                        ) : (
                            <ScrollView style={styles.activityList}>
                                {activity.map((item, index) => (
                                    <View
                                        key={item.id ?? `${index}`}
                                        style={styles.activityItem}
                                    >
                                        <Text style={styles.activityGuest}>
                                            {item.guestName ??
                                                item.name ??
                                                'Guest'}
                                        </Text>
                                        <Text style={styles.activityTime}>
                                            {item.checkInTime ??
                                                item.checkedInAt ??
                                                'Check-in recorded'}
                                        </Text>
                                    </View>
                                ))}
                            </ScrollView>
                        )}
                    </View>
                </View>
            ) : null}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: {
        flex: 1,
        backgroundColor: '#F4F6F7',
    },
    header: {
        height: 58,
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 20,
        backgroundColor: '#FFFFFF',
        borderBottomWidth: 1,
        borderBottomColor: '#DFE5E9',
    },
    backButton: {
        width: 34,
        justifyContent: 'center',
    },
    backIcon: {
        fontSize: 34,
        lineHeight: 38,
        color: '#1B2935',
    },
    headerTitle: {
        marginLeft: 14,
        fontSize: 20,
        fontWeight: '800',
        color: '#1B2935',
    },
    scrollView: {
        flex: 1,
    },
    content: {
        paddingHorizontal: 18,
        paddingTop: 18,
        paddingBottom: 30,
        flexGrow: 1,
    },
    eventCard: {
        backgroundColor: '#FFFFFF',
        borderWidth: 1,
        borderColor: '#DEE5E9',
        borderRadius: 15,
        padding: 17,
        marginBottom: 14,
    },
    eventName: {
        fontSize: 21,
        lineHeight: 28,
        fontWeight: '800',
        color: '#1B2935',
    },
    eventSubtitle: {
        marginTop: 3,
        fontSize: 14,
        lineHeight: 21,
        color: '#657585',
    },
    eventTime: {
        marginTop: 2,
        fontSize: 14,
        color: '#657585',
    },
    countRow: {
        marginTop: 21,
        minHeight: 24,
        justifyContent: 'center',
    },
    countDescription: {
        fontSize: 15,
        color: '#1B2935',
    },
    countNumber: {
        fontSize: 21,
        fontWeight: '800',
    },
    errorContainer: {
        marginTop: 12,
        paddingTop: 10,
        borderTopWidth: 1,
        borderTopColor: '#F0D5D5',
    },
    errorText: {
        fontSize: 13,
        lineHeight: 19,
        color: '#A52D36',
    },
    retryText: {
        marginTop: 7,
        fontSize: 14,
        fontWeight: '800',
        color: '#117A91',
    },
    primaryButton: {
        minHeight: 76,
        borderRadius: 13,
        backgroundColor: '#117A91',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 13,
    },
    primaryButtonText: {
        fontSize: 18,
        fontWeight: '800',
        color: '#FFFFFF',
    },
    secondaryButton: {
        minHeight: 60,
        borderRadius: 13,
        borderWidth: 1,
        borderColor: '#CBD5DC',
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 13,
    },
    secondaryButtonText: {
        fontSize: 16,
        fontWeight: '800',
        color: '#1B2935',
    },
    buttonPressed: {
        opacity: 0.75,
    },
    modalOverlay: {
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        backgroundColor: 'rgba(20, 32, 40, 0.4)',
        justifyContent: 'flex-end',
    },
    activityPanel: {
        maxHeight: '75%',
        minHeight: 220,
        backgroundColor: '#FFFFFF',
        borderTopLeftRadius: 20,
        borderTopRightRadius: 20,
        padding: 20,
        paddingBottom: 32,
    },
    activityHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 18,
    },
    activityTitle: {
        fontSize: 19,
        fontWeight: '800',
        color: '#1B2935',
    },
    closeButton: {
        fontSize: 14,
        fontWeight: '800',
        color: '#117A91',
    },
    emptyActivity: {
        paddingVertical: 25,
        fontSize: 14,
        color: '#657585',
        textAlign: 'center',
    },
    activityList: {
        flexGrow: 0,
    },
    activityItem: {
        paddingVertical: 13,
        borderBottomWidth: 1,
        borderBottomColor: '#E7ECEF',
    },
    activityGuest: {
        fontSize: 15,
        fontWeight: '700',
        color: '#1B2935',
    },
    activityTime: {
        marginTop: 4,
        fontSize: 13,
        color: '#657585',
    },
});