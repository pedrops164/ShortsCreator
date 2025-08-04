'use client';

import React, { createContext, useEffect, useState, ReactNode, useContext } from 'react';
import { VideoStatusUpdate } from '@/types/content';
import { PaymentStatusUpdate } from '@/types/balance';

interface NotificationContextType {
    latestVideoStatus: VideoStatusUpdate | null;
    latestPaymentStatus: PaymentStatusUpdate | null;
    isConnected: boolean;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const NotificationProvider = ({ children }: { children: ReactNode }) => {
    const [latestVideoStatus, setLatestVideoStatus] = useState<VideoStatusUpdate | null>(null);
    const [latestPaymentStatus, setLatestPaymentStatus] = useState<PaymentStatusUpdate | null>(null);
    const [isConnected, setIsConnected] = useState<boolean>(false);

    useEffect(() => {
        // URL to the SSE endpoint, handled by the API gateway
        const eventSource = new EventSource('/api/v1/notifications');

        eventSource.onopen = () => {
            console.log('SSE connection established.');
            setIsConnected(true);
        };

        // Listener for specific video status updates
        eventSource.addEventListener('video_status_update', (event) => {
            console.log('Received video_status_update:', event.data);
            try {
                const data: VideoStatusUpdate = JSON.parse(event.data);
                setLatestVideoStatus(data);
            } catch (error) {
                console.error('Failed to parse video_status_update event data:', error);
            }
        });

        // Listener for payment status updates
        eventSource.addEventListener('payment_status_update', (event) => {
            console.log('Received payment_status_update:', event.data);
            try {
                const data: PaymentStatusUpdate = JSON.parse(event.data);
                setLatestPaymentStatus(data);
            } catch (error) {
                console.error('Failed to parse payment_status_update event data:', error);
            }
        });

        // Listener for the initial 'connected' message from the server
        eventSource.addEventListener('connected', (event) => {
            console.log('Server acknowledgement:', event.data);
        });

        eventSource.onerror = (error) => {
            console.error('SSE error:', error);
            setIsConnected(false);
            // The EventSource API will automatically try to reconnect.
        };

        // Cleanup the connection when the component unmounts
        return () => {
            console.log('Closing SSE connection.');
            eventSource.close();
        };
    }, []); // Empty dependency array ensures this runs only once

    const value = { latestVideoStatus, latestPaymentStatus, isConnected };

    return (
        <NotificationContext.Provider value={value}>
            {children}
        </NotificationContext.Provider>
    );
};

// Custom hook for easy consumption of the context
export const useNotifications = (): NotificationContextType => {
    const context = useContext(NotificationContext);
    if (context === undefined) {
        throw new Error('useNotifications must be used within a NotificationProvider');
    }
    return context;
};