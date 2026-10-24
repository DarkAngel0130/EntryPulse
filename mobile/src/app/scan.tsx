import { useState } from 'react';
import {
    ActivityIndicator,
    Alert,
    Pressable,
    SafeAreaView,
    StatusBar,
    StyleSheet,
    Text,
    TextInput,
    View,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { router, useLocalSearchParams } from 'expo-router';
import * as SecureStore from 'expo-secure-store';
import { API_BASE_URL } from '@/constants/api';

type ScanResult = {
    status: 'VALID' | 'ALREADY_CHECKED_IN' | 'INVALID_FOR_EVENT';
    guestName?: string | null;
    eventName?: string | null;
    checkedInBy?: string | null;
    checkInTime?: string | null;

};


type GuestSearchResult = {
    guestId: string;
    guestName: string;
    eventGuestId: string;
    seatNumber: string | null;
    alreadyCheckedIn: boolean;
    checkedInBy: string | null;
    checkInTime: string | null;
};


export default function ScanScreen() {
    const params = useLocalSearchParams<{
        eventId?: string;
        eventName?: string;
        searchMode?: string;
    }>();

    const eventId = Array.isArray(params.eventId)
        ? params.eventId[0]
        : params.eventId;

    const eventName = Array.isArray(params.eventName)
        ? params.eventName[0]
        : params.eventName;

    const [permission, requestPermission] = useCameraPermissions();
    const [scanned, setScanned] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [result, setResult] = useState<ScanResult | null>(null);
    const [pendingToken, setPendingToken] = useState<string | null>(null);
    const [searchMode, setSearchMode] = useState(
        params.searchMode === 'true'
    );
    const [searchName, setSearchName] = useState('');
    const [searchResults, setSearchResults] = useState<GuestSearchResult[]>([]);
    const [searching, setSearching] = useState(false);
    const [searchError, setSearchError] = useState('');

    async function getAuthToken() {
        const token = await SecureStore.getItemAsync('entrypulse_token');

        if (!token) {
            router.replace('/');
            return null;
        }

        return token;
    }





    function resetScanner() {
        setResult(null);
        setPendingToken(null);
        setScanned(false);
        setSubmitting(false);
    }

    function extractInvitationToken(rawValue: string) {
        const value = rawValue.trim();

        try {
            const url = new URL(value);
            const token = url.searchParams.get('token');

            if (token) return token;
        } catch {
            // The QR value may be a raw invitation token.
        }

        return value;
    }

    async function handleBarcodeScanned({ data }: { data: string }) {
        if (scanned || submitting || result) return;

        setScanned(true);
        setSubmitting(true);

        try {
            if (!eventId) {
                throw new Error('The event ID is missing. Return to My Events and reopen the event.');
            }

            const authToken = await getAuthToken();
            if (!authToken) return;

            const invitationToken = extractInvitationToken(data);

            if (!invitationToken) {
                setResult({ status: 'INVALID_FOR_EVENT' });
                return;
            }

            const response = await fetch(
                `${API_BASE_URL}/api/check-ins/validate`,
                {
                    method: 'POST',
                    headers: {
                        Authorization: `Bearer ${authToken}`,
                        Accept: 'application/json',
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        token: invitationToken,
                        eventId,
                    }),
                }
            );

            if (response.status === 401) {
                await SecureStore.deleteItemAsync('entrypulse_token');
                router.replace('/');
                return;
            }

            if (!response.ok) {
                throw new Error(
                    `Could not validate invitation (HTTP ${response.status}).`
                );
            }

            const dataResult: ScanResult = await response.json();

            if (
                dataResult.status === 'VALID' ||
                dataResult.status === 'ALREADY_CHECKED_IN'
            ) {
                setPendingToken(invitationToken);
                setResult(dataResult);
            } else {
                // Deliberately use the same message for all invalid invitations.
                setPendingToken(null);
                setResult({ status: 'INVALID_FOR_EVENT' });
            }
        } catch (error) {
            Alert.alert(
                'Connection problem',
                error instanceof Error
                    ? error.message
                    : 'Unable to validate the QR code. Please try again.',
                [{ text: 'OK', onPress: resetScanner }]
            );
        } finally {
            setSubmitting(false);
        }
    }

    async function confirmCheckIn() {
        if (!pendingToken || !eventId || submitting) return;

        setSubmitting(true);

        try {
            const authToken = await getAuthToken();
            if (!authToken) return;

            const response = await fetch(`${API_BASE_URL}/api/check-ins`, {
                method: 'POST',
                headers: {
                    Authorization: `Bearer ${authToken}`,
                    Accept: 'application/json',
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ token: pendingToken }),
            });

            const responseData = await response.json().catch(() => null);

            if (response.status === 401) {
                await SecureStore.deleteItemAsync('entrypulse_token');
                router.replace('/');
                return;
            }

            if (!response.ok) {
                // Revalidate so a concurrent check-in can show the
                // correct result instead of incorrectly confirming success.
                const validation = await fetch(
                    `${API_BASE_URL}/api/check-ins/validate`,
                    {
                        method: 'POST',
                        headers: {
                            Authorization: `Bearer ${authToken}`,
                            Accept: 'application/json',
                            'Content-Type': 'application/json',
                        },
                        body: JSON.stringify({
                            token: pendingToken,
                            eventId,
                        }),
                    }
                );

                if (validation.ok) {
                    const latest: ScanResult = await validation.json();

                    if (latest.status === 'ALREADY_CHECKED_IN') {
                        setResult(latest);
                        setPendingToken(null);
                        return;
                    }

                    if (latest.status === 'INVALID_FOR_EVENT') {
                        setResult({ status: 'INVALID_FOR_EVENT' });
                        setPendingToken(null);
                        return;
                    }
                }

                throw new Error(
                    responseData?.detail ??
                    responseData?.message ??
                    'Check-in could not be completed. Please try again.'
                );
            }

            // Return to the camera so the officer can scan the next guest.
            // The event dashboard refreshes when it regains focus.
            resetScanner();

            Alert.alert(
                'Check-in successful',
                `${result?.guestName ?? 'Guest'} has been checked in.`,
                [{ text: 'Scan next' }]
            );
        } catch (error) {
            Alert.alert(
                'Check-in failed',
                error instanceof Error
                    ? error.message
                    : 'Please try again.',
                [{ text: 'OK' }]
            );
        } finally {
            setSubmitting(false);
        }
    }

    async function searchGuests() {
        const name = searchName.trim();

        if (!eventId) {
            setSearchError('Event information is missing. Return to My Events.');
            return;
        }

        if (name.length < 2) {
            setSearchError('Enter at least two characters to search.');
            return;
        }

        setSearching(true);
        setSearchError('');
        setSearchResults([]);

        try {
            const authToken = await getAuthToken();
            if (!authToken) return;

            const response = await fetch(
                `${API_BASE_URL}/api/check-ins/event/${encodeURIComponent(eventId)}/guests/search?name=${encodeURIComponent(name)}`,
                {
                    method: 'GET',
                    headers: {
                        Authorization: `Bearer ${authToken}`,
                        Accept: 'application/json',
                    },
                }
            );

            if (response.status === 401) {
                await SecureStore.deleteItemAsync('entrypulse_token');
                router.replace('/');
                return;
            }

            if (!response.ok) {
                throw new Error(
                    `Guest search failed (HTTP ${response.status}).`
                );
            }

            const guests: GuestSearchResult[] = await response.json();
            setSearchResults(guests);
        } catch (error) {
            setSearchError(
                error instanceof Error
                    ? error.message
                    : 'Unable to search guests. Please try again.'
            );
        } finally {
            setSearching(false);
        }
    }

    function cancelScan() {
        router.back();
    }

    if (!permission && !searchMode) {
        return (
            <View style={styles.permissionScreen}>
                <ActivityIndicator color="#FFFFFF" size="large" />
                <Text style={styles.permissionText}>
                    Checking camera permission...
                </Text>
            </View>
        );
    }

    if (permission && !permission.granted && !searchMode) {
        return (
            <SafeAreaView style={styles.permissionScreen}>
                <Text style={styles.permissionTitle}>
                    Camera access required
                </Text>
                <Text style={styles.permissionText}>
                    EntryPulse needs camera access to scan guest invitations.
                </Text>

                <Pressable
                    style={styles.permissionButton}
                    onPress={() => void requestPermission()}
                >
                    <Text style={styles.permissionButtonText}>
                        Allow camera access
                    </Text>
                </Pressable>

                <Pressable onPress={cancelScan}>
                    <Text style={styles.cancelPermission}>Cancel</Text>
                </Pressable>
            </SafeAreaView>
        );
    }

    if (result?.status === 'VALID') {
        return (
            <SafeAreaView style={[styles.resultScreen, styles.validScreen]}>
                <StatusBar barStyle="light-content" backgroundColor="#165634" />

                <Text style={styles.resultEventBadge}>
                    {eventName || 'EntryPulse Event'}
                </Text>

                <View style={styles.resultContent}>
                    <Text style={styles.successIcon}>✓</Text>
                    <Text style={styles.resultTitle}>Valid invitation</Text>
                    <Text style={styles.resultGuest}>
                        {result.guestName || 'Guest'}
                    </Text>
                    <Text style={styles.resultDetails}>
                        Invitation verified for this event
                    </Text>
                </View>

                <View style={styles.resultActions}>
                    <Pressable
                        style={styles.confirmButton}
                        disabled={submitting}
                        onPress={() => void confirmCheckIn()}
                    >
                        {submitting ? (
                            <ActivityIndicator color="#1B2935" />
                        ) : (
                            <Text style={styles.confirmButtonText}>
                                Confirm check-in
                            </Text>
                        )}
                    </Pressable>

                    <Pressable
                        style={styles.validCancelButton}
                        onPress={cancelScan}
                    >
                        <Text style={styles.validCancelText}>Cancel</Text>
                    </Pressable>
                </View>
            </SafeAreaView>
        );
    }

    if (result?.status === 'ALREADY_CHECKED_IN') {
        return (
            <SafeAreaView style={[styles.resultScreen, styles.alreadyScreen]}>
                <StatusBar barStyle="light-content" backgroundColor="#79350F" />

                <Text style={styles.resultEventBadge}>
                    {eventName || 'EntryPulse Event'}
                </Text>

                <View style={styles.resultContent}>
                    <Text style={styles.clockIcon}>◷</Text>
                    <Text style={styles.resultTitle}>Already checked in</Text>
                    <Text style={styles.resultGuest}>
                        {result.guestName || 'Guest'}
                    </Text>
                    <Text style={styles.resultDetails}>
                        {result.checkInTime
                            ? `Checked in at ${new Date(result.checkInTime).toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                            })}`
                            : 'This guest has already been checked in.'}
                        {result.checkedInBy
                            ? ` by ${result.checkedInBy}`
                            : ''}
                    </Text>
                </View>

                <View style={styles.resultActions}>
                    <Pressable
                        style={styles.confirmButton}
                        onPress={resetScanner}
                    >
                        <Text style={styles.confirmButtonText}>Scan next</Text>
                    </Pressable>
                </View>
            </SafeAreaView>
        );
    }

    if (result?.status === 'INVALID_FOR_EVENT') {
        return (
            <SafeAreaView style={[styles.resultScreen, styles.invalidScreen]}>
                <StatusBar barStyle="light-content" backgroundColor="#851E1E" />

                <Text style={styles.resultEventBadge}>
                    {eventName || 'EntryPulse Event'}
                </Text>

                <View style={styles.resultContent}>
                    <Text style={styles.invalidIcon}>×</Text>
                    <Text style={styles.resultTitle}>
                        Incorrect QR code
                    </Text>
                    <Text style={styles.resultGuest}>
                        Not valid for this event
                    </Text>
                    <Text style={styles.resultDetails}>
                        This QR code cannot be used for this event. Do not admit.
                    </Text>
                </View>

                <View style={styles.resultActions}>
                    <Pressable
                        style={styles.confirmButton}
                        onPress={resetScanner}
                    >
                        <Text style={styles.confirmButtonText}>Scan next</Text>
                    </Pressable>
                </View>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.container}>
            <StatusBar barStyle="light-content" backgroundColor="#0B1419" />

            <View style={styles.topBar}>
                <Text style={styles.eventBadge}>
                    {eventName || 'EntryPulse Event'}
                </Text>

                <Pressable
                    style={styles.cancelButton}
                    onPress={cancelScan}
                >
                    <Text style={styles.cancelText}>Cancel</Text>
                </Pressable>
            </View>


            {searchMode ? (
                <View style={styles.searchArea}>
                    <Text style={styles.searchTitle}>Find a guest</Text>

                    <Text style={styles.searchSubtitle}>
                        Search the registered guest list for this event.
                        Inspect the guest's invitation proof before admitting them.
                    </Text>

                    <View style={styles.searchInputRow}>
                        <TextInput
                            style={styles.searchInput}
                            placeholder="Enter guest name..."
                            placeholderTextColor="#8B9AA5"
                            value={searchName}
                            onChangeText={setSearchName}
                            onSubmitEditing={() => void searchGuests()}
                            returnKeyType="search"
                            autoCapitalize="words"
                            autoCorrect={false}
                            accessibilityLabel="Search registered guests by name"
                        />

                        <Pressable
                            style={[
                                styles.searchAction,
                                searching && styles.disabledButton,
                            ]}
                            onPress={() => void searchGuests()}
                            disabled={searching}
                        >
                            {searching ? (
                                <ActivityIndicator color="#FFFFFF" />
                            ) : (
                                <Text style={styles.searchActionText}>Search</Text>
                            )}
                        </Pressable>
                    </View>

                    {searchError !== '' && (
                        <Text style={styles.searchError}>{searchError}</Text>
                    )}

                    {!searching &&
                        searchName.trim().length >= 2 &&
                        searchResults.length === 0 &&
                        searchError === '' && (
                            <Text style={styles.emptySearch}>
                                No matching registered guests found.
                            </Text>
                        )}

                    <View style={styles.searchResults}>
                        {searchResults.map((guest) => (
                            <View
                                key={guest.eventGuestId}
                                style={styles.guestResultCard}
                            >
                                <View style={styles.guestResultHeader}>
                                    <View style={styles.guestInfo}>
                                        <Text style={styles.guestResultName}>
                                            {guest.guestName}
                                        </Text>
                                        <Text style={styles.guestResultDetails}>
                                            {guest.seatNumber
                                                ? `Seat: ${guest.seatNumber}`
                                                : 'No seat assigned'}
                                        </Text>
                                    </View>

                                    <View
                                        style={[
                                            styles.guestStatus,
                                            guest.alreadyCheckedIn
                                                ? styles.checkedInStatus
                                                : styles.pendingStatus,
                                        ]}
                                    >
                                        <Text style={styles.guestStatusText}>
                                            {guest.alreadyCheckedIn
                                                ? 'Checked in'
                                                : 'Not checked in'}
                                        </Text>
                                    </View>
                                </View>

                                {guest.alreadyCheckedIn ? (
                                    <Text style={styles.guestResultDetails}>
                                        {guest.checkInTime
                                            ? `Time: ${new Date(
                                                guest.checkInTime
                                            ).toLocaleString()}`
                                            : 'Check-in time unavailable'}
                                        {guest.checkedInBy
                                            ? `\nOfficer: ${guest.checkedInBy}`
                                            : ''}
                                    </Text>
                                ) : (
                                    <Text style={styles.proofReminder}>
                                        Verify the guest's identity and invitation proof
                                        before check-in.
                                    </Text>
                                )}
                            </View>
                        ))}
                    </View>
                </View>
            ) : (
                <View style={styles.cameraArea}>
                    <View style={styles.scanFrame}>
                        <CameraView
                            style={styles.camera}
                            facing="back"
                            barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
                            onBarcodeScanned={
                                scanned || submitting || result
                                    ? undefined
                                    : handleBarcodeScanned
                            }
                        />

                        <View pointerEvents="none" style={styles.scanOverlay}>
                            <View style={[styles.corner, styles.topLeft]} />
                            <View style={[styles.corner, styles.topRight]} />
                            <View style={[styles.corner, styles.bottomLeft]} />
                            <View style={[styles.corner, styles.bottomRight]} />
                        </View>
                    </View>

                    <Text style={styles.instruction}>
                        Point the camera at the guest invitation QR code
                    </Text>

                    {submitting && (
                        <View style={styles.processing}>
                            <ActivityIndicator color="#FFFFFF" />
                            <Text style={styles.processingText}>
                                Validating invitation...
                            </Text>
                        </View>
                    )}
                </View>
            )}


            <View style={styles.bottomArea}>
                <Pressable
                    style={styles.guestButton}
                    onPress={() => {
                        if (searchMode) {
                            setSearchMode(false);
                            setSearchName('');
                            setSearchResults([]);
                            setSearchError('');
                        } else {
                            setSearchMode(true);
                            setSearchName('');
                            setSearchResults([]);
                            setSearchError('');
                        }
                    }}
                >
                    <Text style={styles.guestButtonText}>
                        {searchMode ? 'Back to QR scanner' : 'Find a guest by name'}
                    </Text>
                </Pressable>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#0B1419',
    },
    topBar: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 18,
        paddingTop: 16,
    },
    eventBadge: {
        overflow: 'hidden',
        borderRadius: 20,
        backgroundColor: '#DDF1F5',
        color: '#12627A',
        fontSize: 13,
        fontWeight: '700',
        paddingHorizontal: 11,
        paddingVertical: 5,
        maxWidth: '68%',
    },
    cancelButton: {
        paddingHorizontal: 18,
        paddingVertical: 14,
        borderRadius: 13,
        backgroundColor: '#25333D',
    },
    cancelText: {
        color: '#FFFFFF',
        fontSize: 15,
        fontWeight: '700',
    },
    cameraArea: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 20,
    },
    scanFrame: {
        width: '76%',
        aspectRatio: 1,
        overflow: 'hidden',
        borderRadius: 17,
        backgroundColor: '#17232C',
    },
    camera: {
        flex: 1,
    },
    scanOverlay: {
        position: 'absolute',
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
    },
    corner: {
        position: 'absolute',
        width: 35,
        height: 35,
        borderColor: '#48D5E8',
    },
    topLeft: {
        top: 0,
        left: 0,
        borderTopWidth: 4,
        borderLeftWidth: 4,
        borderTopLeftRadius: 12,
    },
    topRight: {
        top: 0,
        right: 0,
        borderTopWidth: 4,
        borderRightWidth: 4,
        borderTopRightRadius: 12,
    },
    bottomLeft: {
        bottom: 0,
        left: 0,
        borderBottomWidth: 4,
        borderLeftWidth: 4,
        borderBottomLeftRadius: 12,
    },
    bottomRight: {
        bottom: 0,
        right: 0,
        borderBottomWidth: 4,
        borderRightWidth: 4,
        borderBottomRightRadius: 12,
    },
    instruction: {
        marginTop: 26,
        textAlign: 'center',
        fontSize: 15,
        lineHeight: 22,
        color: '#B8C5CE',
    },
    processing: {
        marginTop: 18,
        flexDirection: 'row',
        alignItems: 'center',
        gap: 10,
    },
    processingText: {
        color: '#FFFFFF',
        fontSize: 14,
    },
    bottomArea: {
        paddingHorizontal: 19,
        paddingTop: 12,
        paddingBottom: 20,
    },
    guestButton: {
        minHeight: 60,
        borderRadius: 13,
        backgroundColor: '#25333D',
        alignItems: 'center',
        justifyContent: 'center',
    },
    guestButtonText: {
        fontSize: 16,
        fontWeight: '800',
        color: '#FFFFFF',
    },
    searchArea: {
        flex: 1,
        paddingHorizontal: 19,
        paddingTop: 20,
        paddingBottom: 12,
    },
    searchTitle: {
        color: '#FFFFFF',
        fontSize: 25,
        fontWeight: '800',
    },
    searchSubtitle: {
        color: '#B8C5CE',
        fontSize: 14,
        lineHeight: 21,
        marginTop: 8,
        marginBottom: 18,
    },
    searchInputRow: {
        flexDirection: 'row',
        gap: 8,
    },
    searchInput: {
        flex: 1,
        minWidth: 0,
        minHeight: 52,
        backgroundColor: '#FFFFFF',
        borderRadius: 12,
        paddingHorizontal: 14,
        color: '#17232C',
        fontSize: 16,
    },
    searchAction: {
        minWidth: 84,
        paddingHorizontal: 14,
        borderRadius: 12,
        backgroundColor: '#117A91',
        alignItems: 'center',
        justifyContent: 'center',
    },
    searchActionText: {
        color: '#FFFFFF',
        fontSize: 15,
        fontWeight: '800',
    },
    disabledButton: {
        opacity: 0.6,
    },
    searchError: {
        color: '#FF9999',
        fontSize: 14,
        marginTop: 12,
    },
    emptySearch: {
        color: '#B8C5CE',
        fontSize: 14,
        textAlign: 'center',
        marginTop: 25,
    },
    searchResults: {
        flex: 1,
        marginTop: 16,
    },
    guestResultCard: {
        backgroundColor: '#17232C',
        borderRadius: 13,
        padding: 14,
        marginBottom: 10,
        gap: 10,
    },
    guestResultHeader: {
        flexDirection: 'row',
        alignItems: 'flex-start',
        justifyContent: 'space-between',
        gap: 8,
    },
    guestInfo: {
        flex: 1,
    },
    guestResultName: {
        color: '#FFFFFF',
        fontSize: 17,
        fontWeight: '800',
    },
    guestResultDetails: {
        color: '#B8C5CE',
        fontSize: 13,
        lineHeight: 19,
        marginTop: 5,
    },
    guestStatus: {
        paddingHorizontal: 9,
        paddingVertical: 6,
        borderRadius: 8,
    },
    checkedInStatus: {
        backgroundColor: '#24583D',
    },
    pendingStatus: {
        backgroundColor: '#55451D',
    },
    guestStatusText: {
        color: '#FFFFFF',
        fontSize: 11,
        fontWeight: '800',
    },
    proofReminder: {
        color: '#F0D69A',
        fontSize: 13,
        lineHeight: 19,
    },
    resultScreen: {
        flex: 1,
        paddingHorizontal: 17,
        paddingTop: 18,
        paddingBottom: 22,
        justifyContent: 'space-between',
    },
    validScreen: {
        backgroundColor: '#165634',
    },
    alreadyScreen: {
        backgroundColor: '#79350F',
    },
    invalidScreen: {
        backgroundColor: '#851E1E',
    },
    resultEventBadge: {
        alignSelf: 'flex-start',
        overflow: 'hidden',
        borderRadius: 20,
        backgroundColor: 'rgba(255,255,255,0.20)',
        color: '#FFFFFF',
        fontSize: 13,
        fontWeight: '700',
        paddingHorizontal: 11,
        paddingVertical: 5,
        maxWidth: '100%',
    },
    resultContent: {
        alignItems: 'center',
        paddingHorizontal: 10,
    },
    successIcon: {
        color: '#FFFFFF',
        fontSize: 78,
        lineHeight: 88,
        fontWeight: '400',
        marginBottom: 12,
    },
    clockIcon: {
        color: '#FFFFFF',
        fontSize: 76,
        lineHeight: 88,
        marginBottom: 12,
    },
    invalidIcon: {
        width: 68,
        height: 68,
        borderRadius: 34,
        borderWidth: 5,
        borderColor: '#FFFFFF',
        color: '#FFFFFF',
        textAlign: 'center',
        textAlignVertical: 'center',
        fontSize: 56,
        lineHeight: 64,
        fontWeight: '400',
        overflow: 'hidden',
        marginBottom: 20,
    },
    resultTitle: {
        color: '#FFFFFF',
        fontSize: 29,
        lineHeight: 37,
        fontWeight: '800',
        textAlign: 'center',
    },
    resultGuest: {
        color: '#FFFFFF',
        fontSize: 21,
        lineHeight: 29,
        fontWeight: '700',
        textAlign: 'center',
        marginTop: 12,
    },
    resultDetails: {
        color: '#FFFFFF',
        fontSize: 15,
        lineHeight: 23,
        textAlign: 'center',
        marginTop: 12,
    },
    resultActions: {
        gap: 12,
    },
    confirmButton: {
        minHeight: 59,
        borderRadius: 13,
        backgroundColor: '#FFFFFF',
        alignItems: 'center',
        justifyContent: 'center',
        paddingHorizontal: 16,
    },
    confirmButtonText: {
        color: '#1B2935',
        fontSize: 16,
        fontWeight: '800',
    },
    validCancelButton: {
        minHeight: 59,
        borderRadius: 13,
        borderWidth: 2,
        borderColor: 'rgba(255,255,255,0.7)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    validCancelText: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '800',
    },
    permissionScreen: {
        flex: 1,
        backgroundColor: '#0B1419',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 25,
    },
    permissionTitle: {
        fontSize: 21,
        fontWeight: '800',
        color: '#FFFFFF',
        marginBottom: 12,
    },
    permissionText: {
        marginTop: 12,
        marginBottom: 22,
        textAlign: 'center',
        fontSize: 15,
        lineHeight: 23,
        color: '#B8C5CE',
    },
    permissionButton: {
        backgroundColor: '#117A91',
        paddingHorizontal: 22,
        paddingVertical: 15,
        borderRadius: 12,
        marginBottom: 20,
    },
    permissionButtonText: {
        color: '#FFFFFF',
        fontWeight: '800',
    },
    cancelPermission: {
        color: '#FFFFFF',
        fontWeight: '700',
        padding: 10,
    },
});
