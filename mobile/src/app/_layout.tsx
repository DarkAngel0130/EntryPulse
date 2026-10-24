
import { Stack } from 'expo-router';

export default function RootLayout() {
    return (
        <Stack screenOptions={{ headerShown: false }}>
            <Stack.Screen name="index" />
            <Stack.Screen name="register" />
            <Stack.Screen name="home" />
            <Stack.Screen name="(security)" />
            <Stack.Screen name="event" />
            <Stack.Screen name="scan" />
        </Stack>
    );
}
