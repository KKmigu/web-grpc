export default class ToastComponent {
    constructor() {
        this.toastContainer = document.createElement('div');
        document.body.appendChild(this.toastContainer);
    }

    createToast(type, message) {
        const toast = document.createElement('div');
        toast.className = 'bg-white border border-gray-200 rounded-xl shadow-md break-words wrap';
        toast.setAttribute('role', 'alert');
        toast.style.position = 'absolute';
        toast.style.top = '100px';
        toast.style.animation = 'slide-up 0.3s ease-out forwards';

        const flexContainer = document.createElement('div');
        flexContainer.className = 'flex p-4';

        const iconContainer = document.createElement('div');
        iconContainer.className = 'flex-shrink-0';

        let iconColor;
        let iconPath;

        switch (type) {
            case 'normal':
                iconColor = 'text-blue-500';
                iconPath = this.getNormalIconPath();
                break;
            case 'success':
                iconColor = 'text-green-500';
                iconPath = this.getSuccessIconPath();
                break;
            case 'error':
                iconColor = 'text-red-500';
                iconPath = this.getErrorIconPath();
                break;
            case 'warning':
                iconColor = 'text-yellow-500';
                iconPath = this.getWarningIconPath();
                break;
            default:
                throw new Error('Invalid toast type');
        }

        const icon = this.createSvgIcon(iconColor, iconPath);
        iconContainer.appendChild(icon);

        const messageContainer = document.createElement('div');
        messageContainer.className = 'ms-3';
        const p = document.createElement('p');
        p.className = 'text-sm text-gray-700 w-60';
        p.textContent = message;
        messageContainer.appendChild(p);

        flexContainer.appendChild(iconContainer);
        flexContainer.appendChild(messageContainer);

        toast.appendChild(flexContainer);

        this.toastContainer.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slide-down 0.3s ease-out forwards';
            setTimeout(() => {
                this.toastContainer.removeChild(toast);
            }, 1000);
        }, 3000);
    }

    createSvgIcon(colorClass, pathData) {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('class', `flex-shrink-0 size-4 ${colorClass} mt-0.5`);
        svg.setAttribute('width', '16');
        svg.setAttribute('height', '16');
        svg.setAttribute('fill', 'currentColor');
        svg.setAttribute('viewBox', '0 0 16 16');
        const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        path.setAttribute('d', pathData);
        svg.appendChild(path);
        return svg;
    }

    // Define the SVG paths for each type of icon
    getNormalIconPath() {
        return "M8 16A8 8 0 1 0 8 0a8 8 0 0 0 0 16zm.93-9.412-1 4.705c-.07.34.029.533.304.533.194 0 .487-.07.686-.246l-.088.416c-.287.346-.92.598-1.465.598-.703 0-1.002-.422-.808-1.319l.738-3.468c.064-.293.006-.399-.287-.47l-.451-.081.082-.381 2.29-.287zM8 5.5a1 1 0 1 1 0-2 1 1 0 0 1 0 2z";
    }

    getSuccessIconPath() {
        return "M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zm-3.97-3.03a.75.75 0 0 0-1.08.022L7.477 9.417 5.384 7.323a.75.75 0 0 0-1.06 1.06L6.97 11.03a.75.75 0 0 0 1.079-.02l3.992-4.99a.75.75 0 0 0-.01-1.05z";
    }

    getErrorIconPath() {
        return "M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM5.354 4.646a.5.5 0 1 0-.708.708L7.293 8l-2.647 2.646a.5.5 0 0 0 .708.708L8 8.707l2.646 2.647a.5.5 0 0 0 .708-.708L8.707 8l2.647-2.646a.5.5 0 0 0-.708-.708L8 7.293 5.354 4.646z";
    }

    getWarningIconPath() {
        return "M16 8A8 8 0 1 1 0 8a8 8 0 0 1 16 0zM8 4a.905.905 0 0 0-.9.995l.35 3.507a.552.552 0 0 0 1.1 0l.35-3.507A.905.905 0 0 0 8 4zm.002 6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z";
    }
}

const style = document.createElement('style');
style.textContent = `
          @keyframes slide-up {
            from {
              opacity: 0;
              transform: translateY(100%);
            }
            to {
              opacity: 1;
              transform: translateY(0);
            }
          }
          @keyframes slide-down {
            from {
              opacity: 1;
              transform: translateY(0);
            }
            to {
              opacity: 0;
              transform: translateY(100%);
            }
          }
        `;
document.head.appendChild(style);

