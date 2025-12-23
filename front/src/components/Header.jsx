import {
    MessageSquare,
} from 'lucide-react';

export const Header = ({ headerText, paragraphText}) => {
    /* Header do Chat */
    /* Mantém azul/branco mesmo no modo escuro */

    return (
        <div className="bg-blue-600 p-6 border-none rounded-2xl m-4 text-white shrink-0 items-center justify-center flex flex-col">
            <h2 className="text-xl font-bold flex items-center gap-2">
                <MessageSquare size={20} />
                {headerText}
            </h2>
            <p className={`text-sm`}>{paragraphText}</p>
        </div>
    )
}