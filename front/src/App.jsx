import { useEffect, useState } from 'react';
import axios from 'axios';
import { Sidebar } from './pages/home/Sidebar';
import { Main } from './pages/home/Main';
import { darkModeBackground } from './shared/constants';

const App = () => {
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [chats, setChats] = useState([
    { id: 1, title: 'Análise Contrato_A.pdf', active: true },
    { id: 2, title: 'Resumo Relatório_RH.pdf', active: false },
    { id: 3, title: 'Docs Fiscais 2024.doc', active: false },
  ]);

  const fetchBack = async () => {
    const response = await axios.get('http://localhost:8080/hello')
    console.log(response.data)
  }
  useEffect(() => {
    fetchBack();
  }, [])

  const { pageBgAndText } = darkModeBackground(isDarkMode);


  return (
    <div className={`flex h-screen transition-colors ${pageBgAndText}`}>
      <Sidebar chats={chats} isDarkMode={isDarkMode} />
      <Main isDarkMode={isDarkMode}/>
    </div>
  );
};

export default App;