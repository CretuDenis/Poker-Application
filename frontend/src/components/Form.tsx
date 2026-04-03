import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { ACCESS_TOKEN, REFRESH_TOKEN } from '../constants';

type Method = 'login' | 'register';

interface FormProps {
  route: string;
  method: Method;
}

interface AuthResponse {
  access: string;
  refresh: string;
}

function Form({ route, method }: FormProps) {
    const [username, setUsername] = useState<string>('');
    const [password, setPassword] = useState<string>('');
    const [loading, setLoading] = useState<boolean>(false);
    const name = method === 'login' ? 'Login' : 'Register';
    const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>): Promise<void> => {
    setLoading(true);
    e.preventDefault();
    try {
      const res = await api.post<AuthResponse>(route, { username, password });

      if (method === 'login') {
        localStorage.setItem(ACCESS_TOKEN, res.data.access);
        localStorage.setItem(REFRESH_TOKEN, res.data.refresh);
        navigate('/');
      } else {
        navigate('/login');
      }
    } catch (error) {
      alert(error);
    } finally {
      setLoading(false);
    }
  };

    if(method === "register") {
          return (
              <div>
                <h2>{name}</h2>
                  <form onSubmit={handleSubmit}>
                    <label>Username</label>
                    <input value={username} onChange = {(e) => setUsername(e.target.value)} type="text" id="username" placeholder="Enter your username" required />
                    <label>Password</label>
                    <input value={password} onChange = {(e) => setPassword(e.target.value)} type="password" id="password" placeholder="Enter your password" required />
                    <button type="submit">{loading ? '...' : name}</button>
                  </form>
              </div>
          );

    }

  return (
      <div>
        <h2>{name}</h2>
          <form onSubmit={handleSubmit}>
            <label>Username</label>
            <input value={username} onChange = {(e) => setUsername(e.target.value)} type="text" id="username" placeholder="Enter your username" required />

            <label>Password</label>
            <input value={password} onChange = {(e) => setPassword(e.target.value)} type="password" id="password" placeholder="Enter your password" required />

            <button type="submit">{loading ? '...' : name}</button>
          </form>
          <p>Don't have an account? <a href="/register">Register</a></p>
      </div>
  );
}

export default Form;
