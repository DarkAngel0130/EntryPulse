import { useState } from 'react';
import {
    Alert,
    KeyboardAvoidingView,
    Platform,
    Pressable,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { API_BASE_URL } from '@/constants/api';

export default function RegisterScreen() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);

    async function handleRegister() {
        if (
            !name.trim() ||
            !email.trim() ||
            !password ||
            !confirmPassword
        ) {
            Alert.alert(
                'Missing information',
                'Please complete all required fields.'
            );
            return;
        }

        if (password !== confirmPassword) {
            Alert.alert(
                'Password mismatch',
                'Your passwords do not match.'
            );
            return;
        }

        try {
            const response = await fetch(
                `${API_BASE_URL}/api/auth/register/security`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        name: name.trim(),
                        email: email.trim().toLowerCase(),
                        password,
                        phoneNumber: phoneNumber.trim(),
                    }),
                }
            );

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                throw new Error(
                    data?.message ||
                    data?.error ||
                    `Registration failed (${response.status}).`
                );
            }

            Alert.alert(
                'Registration successful',
                'Your security account has been created. You can now sign in.',
                [
                    {
                        text: 'Sign in',
                        onPress: () => router.replace('/'),
                    },
                ]
            );
        } catch (error) {
            const message =
                error instanceof Error
                    ? error.message
                    : 'Unable to connect to the server.';

            Alert.alert('Registration failed', message);
        }
    }

    return (
        <SafeAreaView style={styles.safeArea}>
            <KeyboardAvoidingView
                style={styles.flex}
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
            >
                <ScrollView
                    contentContainerStyle={styles.scrollContent}
                    keyboardShouldPersistTaps="handled"
                >
                    <View style={styles.container}>
                        <View style={styles.brandSection}>
                            <View style={styles.logo}>
                                <Text style={styles.logoText}>E</Text>
                            </View>
                            <Text style={styles.brandName}>EntryPulse</Text>
                            <Text style={styles.brandSubtitle}>SECURITY REGISTRATION</Text>
                        </View>

                        <View style={styles.formSection}>
                            <Text style={styles.heading}>Create your account</Text>
                            <Text style={styles.description}>
                                Register as a security officer to receive event invitations.
                            </Text>

                            <Text style={styles.label}>Full name *</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="Enter your full name"
                                placeholderTextColor="#929BA5"
                                value={name}
                                onChangeText={setName}
                                autoCapitalize="words"
                                autoComplete="name"
                            />

                            <Text style={styles.label}>Email address *</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="Enter your email"
                                placeholderTextColor="#929BA5"
                                value={email}
                                onChangeText={setEmail}
                                keyboardType="email-address"
                                autoCapitalize="none"
                                autoCorrect={false}
                                autoComplete="email"
                            />

                            <Text style={styles.label}>Phone number</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="Enter your phone number"
                                placeholderTextColor="#929BA5"
                                value={phoneNumber}
                                onChangeText={setPhoneNumber}
                                keyboardType="phone-pad"
                                autoComplete="tel"
                            />

                            <Text style={styles.label}>Password *</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="Create a password"
                                placeholderTextColor="#929BA5"
                                value={password}
                                onChangeText={setPassword}
                                secureTextEntry={!showPassword}
                                autoCapitalize="none"
                                autoComplete="new-password"
                            />

                            <Text style={styles.label}>Confirm password *</Text>
                            <TextInput
                                style={styles.input}
                                placeholder="Re-enter your password"
                                placeholderTextColor="#929BA5"
                                value={confirmPassword}
                                onChangeText={setConfirmPassword}
                                secureTextEntry={!showPassword}
                                autoCapitalize="none"
                                autoComplete="new-password"
                            />

                            <Pressable
                                onPress={() => setShowPassword(!showPassword)}
                                style={styles.visibilityButton}
                            >
                                <Text style={styles.showPassword}>
                                    {showPassword ? 'Hide passwords' : 'Show passwords'}
                                </Text>
                            </Pressable>

                            <Pressable
                                style={styles.registerButton}
                                onPress={handleRegister}
                            >
                                <Text style={styles.registerButtonText}>Create account</Text>
                            </Pressable>

                            <View style={styles.loginRow}>
                                <Text style={styles.loginText}>Already registered? </Text>
                                <Pressable onPress={() => router.replace('/')}>
                                    <Text style={styles.loginLink}>Sign in</Text>
                                </Pressable>
                            </View>
                        </View>
                    </View>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    flex: { flex: 1 },
    safeArea: { flex: 1, backgroundColor: '#F5F7F8' },
    scrollContent: { flexGrow: 1 },
    container: {
        flex: 1,
        paddingHorizontal: 26,
        paddingVertical: 28,
    },
    brandSection: {
        alignItems: 'center',
        marginBottom: 28,
    },
    logo: {
        width: 54,
        height: 54,
        borderRadius: 16,
        backgroundColor: '#0D9488',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 10,
    },
    logoText: { fontSize: 30, fontWeight: '800', color: '#FFFFFF' },
    brandName: {
        fontSize: 26,
        fontWeight: '800',
        color: '#15313B',
        letterSpacing: -0.7,
    },
    brandSubtitle: {
        marginTop: 5,
        fontSize: 10,
        fontWeight: '700',
        letterSpacing: 2,
        color: '#0D9488',
    },
    formSection: { width: '100%', maxWidth: 440, alignSelf: 'center' },
    heading: {
        fontSize: 25,
        fontWeight: '800',
        color: '#172B36',
        marginBottom: 7,
    },
    description: {
        fontSize: 14,
        lineHeight: 20,
        color: '#667581',
        marginBottom: 22,
    },
    label: {
        fontSize: 13,
        fontWeight: '700',
        color: '#263A45',
        marginBottom: 7,
    },
    input: {
        minHeight: 49,
        borderWidth: 1,
        borderColor: '#DCE3E7',
        borderRadius: 11,
        backgroundColor: '#FFFFFF',
        paddingHorizontal: 14,
        fontSize: 14,
        color: '#172B36',
        marginBottom: 15,
    },
    visibilityButton: {
        alignSelf: 'flex-end',
        paddingVertical: 4,
        marginTop: -9,
        marginBottom: 16,
    },
    showPassword: {
        color: '#0D9488',
        fontSize: 13,
        fontWeight: '700',
    },
    registerButton: {
        minHeight: 52,
        borderRadius: 12,
        backgroundColor: '#0D9488',
        alignItems: 'center',
        justifyContent: 'center',
    },
    registerButtonText: {
        fontSize: 15,
        fontWeight: '700',
        color: '#FFFFFF',
    },
    loginRow: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        flexWrap: 'wrap',
        marginTop: 20,
    },
    loginText: { fontSize: 13, color: '#667581' },
    loginLink: { fontSize: 13, fontWeight: '700', color: '#0D9488' },
});