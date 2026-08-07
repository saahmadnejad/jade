// Example API client using Axios to interact with Spring Boot backend
import axios from 'axios';

const apiClient = axios.create({
    baseURL: 'http://localhost:8080/api', // Adjust to your Spring Boot backend URL
    headers: {
        'Content-Type': 'application/json',
    },
});

// Example API call
export const fetchData = async (endpoint: string) => {
    try {
        const response = await apiClient.get(endpoint);
        return response.data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
};

// Add more API methods as needed
export const postData = async (endpoint: string, data: any) => {
    try {
        const response = await apiClient.post(endpoint, data);
        return response.data;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
};