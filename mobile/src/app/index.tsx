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
import * as SecureStore from 'expo-secure-store';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  async function handleLogin() {
    if (!email.trim() || !password) {
      Alert.alert(
          'Missing information',
          'Please enter your email and password.'
      );
      return;
    }

    try {
      setIsLoading(true);

      const response = await fetch(`${API_BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email: email.trim().toLowerCase(),
          password,
        }),
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        throw new Error(
            data?.message ||
            data?.error ||
            `Login failed (${response.status}).`
        );
      }

      if (!data?.token || data?.role !== 'SECURITY') {
        Alert.alert(
            'Access denied',
            'This app is for authorised security personnel.'
        );
        return;
      }

      // Save the JWT before navigating to the home screen.
      await SecureStore.setItemAsync('entrypulse_token', data.token);

      // Navigate to the security home screen.
      router.replace('/home');
    } catch (error) {
      const message =
          error instanceof Error
              ? error.message
              : 'Unable to connect to the server.';

      Alert.alert('Sign-in failed', message);
    } finally {
      setIsLoading(false);
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
                <Text style={styles.brandSubtitle}>EVENT SECURITY</Text>
              </View>

              <View style={styles.formSection}>
                <Text style={styles.heading}>Welcome back</Text>
                <Text style={styles.description}>
                  Sign in to manage your assigned events.
                </Text>

                <Text style={styles.label}>Email address</Text>
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

                <Text style={styles.label}>Password</Text>
                <View style={styles.passwordContainer}>
                  <TextInput
                      style={styles.passwordInput}
                      placeholder="Enter your password"
                      placeholderTextColor="#929BA5"
                      value={password}
                      onChangeText={setPassword}
                      secureTextEntry={!showPassword}
                      autoCapitalize="none"
                      autoComplete="current-password"
                  />

                  <Pressable
                      onPress={() => setShowPassword(!showPassword)}
                      accessibilityRole="button"
                      accessibilityLabel={
                        showPassword ? 'Hide password' : 'Show password'
                      }
                  >
                    <Text style={styles.showPassword}>
                      {showPassword ? 'Hide' : 'Show'}
                    </Text>
                  </Pressable>
                </View>

                <Pressable
                    style={[
                      styles.signInButton,
                      isLoading && styles.disabledButton,
                    ]}
                    onPress={handleLogin}
                    disabled={isLoading}
                    accessibilityRole="button"
                >
                  <Text style={styles.signInButtonText}>
                    {isLoading ? 'Signing in...' : 'Sign in'}
                  </Text>
                </Pressable>

                <View style={styles.registerRow}>
                  <Text style={styles.registerText}>
                    Don't have an account?{' '}
                  </Text>

                  <Pressable
                      onPress={() => router.push('/register')}
                      disabled={isLoading}
                      accessibilityRole="button"
                  >
                    <Text style={styles.registerLink}>Register</Text>
                  </Pressable>
                </View>

                <Text style={styles.footer}>
                  Secure access for authorised event personnel.
                </Text>
              </View>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  flex: {
    flex: 1,
  },
  safeArea: {
    flex: 1,
    backgroundColor: '#F5F7F8',
  },
  scrollContent: {
    flexGrow: 1,
    justifyContent: 'center',
  },
  container: {
    flex: 1,
    justifyContent: 'center',
    paddingHorizontal: 26,
    paddingVertical: 36,
  },
  brandSection: {
    alignItems: 'center',
    marginBottom: 38,
  },
  logo: {
    width: 62,
    height: 62,
    borderRadius: 18,
    backgroundColor: '#0D9488',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  logoText: {
    fontSize: 34,
    fontWeight: '800',
    color: '#FFFFFF',
  },
  brandName: {
    fontSize: 28,
    fontWeight: '800',
    color: '#15313B',
    letterSpacing: -0.7,
  },
  brandSubtitle: {
    marginTop: 6,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 2.5,
    color: '#0D9488',
  },
  formSection: {
    width: '100%',
    maxWidth: 440,
    alignSelf: 'center',
  },
  heading: {
    fontSize: 27,
    fontWeight: '800',
    color: '#172B36',
    marginBottom: 8,
  },
  description: {
    fontSize: 14,
    lineHeight: 21,
    color: '#667581',
    marginBottom: 28,
  },
  label: {
    fontSize: 13,
    fontWeight: '700',
    color: '#263A45',
    marginBottom: 8,
  },
  input: {
    height: 52,
    borderWidth: 1,
    borderColor: '#DCE3E7',
    borderRadius: 12,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 15,
    fontSize: 15,
    color: '#172B36',
    marginBottom: 20,
  },
  passwordContainer: {
    minHeight: 52,
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#DCE3E7',
    borderRadius: 12,
    backgroundColor: '#FFFFFF',
    paddingHorizontal: 15,
    marginBottom: 24,
  },
  passwordInput: {
    flex: 1,
    minHeight: 50,
    fontSize: 15,
    color: '#172B36',
  },
  showPassword: {
    color: '#0D9488',
    fontSize: 13,
    fontWeight: '700',
    paddingLeft: 12,
    paddingVertical: 12,
  },
  signInButton: {
    height: 54,
    borderRadius: 12,
    backgroundColor: '#0D9488',
    alignItems: 'center',
    justifyContent: 'center',
  },
  disabledButton: {
    opacity: 0.65,
  },
  signInButtonText: {
    fontSize: 16,
    fontWeight: '700',
    color: '#FFFFFF',
  },
  registerRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    flexWrap: 'wrap',
    marginTop: 20,
  },
  registerText: {
    fontSize: 13,
    color: '#667581',
  },
  registerLink: {
    fontSize: 13,
    fontWeight: '700',
    color: '#0D9488',
  },
  footer: {
    fontSize: 12,
    lineHeight: 18,
    color: '#7B8790',
    textAlign: 'center',
    marginTop: 22,
  },
});