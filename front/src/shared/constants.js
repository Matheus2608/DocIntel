export const darkModeBackground = (isDarkMode) => {
  const darkModeBg = isDarkMode ? 'bg-[#444654]' : 'bg-white';
  const pageBgAndText = isDarkMode ? 'bg-[#343541] text-gray-100' : 'bg-gray-50 text-gray-800';
  const borderColor = isDarkMode ? 'border-gray-600' : 'border-gray-300';

  return { darkModeBg, pageBgAndText, borderColor };
};