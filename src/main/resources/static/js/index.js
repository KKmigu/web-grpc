import ToastComponent from './toast.js'

const BASE_URL = window.location.href

const QUERY_DESCRIPTOR_URL = BASE_URL + 'api/grpc/query'

const GRPC_CALL_URL = BASE_URL + 'api/grpc/call'

const descriptorData = []
const toastComponent = new ToastComponent()

const queryDescriptorBtnContainer = document.getElementById('compile')
const addressInputContainer = document.getElementById('address')
const protInputContainer = document.getElementById('port')
const fullServiceInputContainer = document.getElementById('fullService')
const methodSelectContainer = document.getElementById('methodSelect')
const dataTableContainer = document.getElementById('dataTable')

const proxyAddressInputContainer = document.getElementById('p_address')
const proxyPortInputContainer = document.getElementById('p_port')

const loadingContainer = document.getElementById('loading')

const commitBtnContainer = document.getElementById('call')
const resultContainer = document.getElementById('result');

const fileInputBoxContainer = document.getElementById('fileInputBox');
const transportTypeRadioContainers = document.getElementsByName('transportType')

const reqInstance = {
    address: null,
    port: null,
    fullService: null,
    method: null,
    message: null,
    fieldList: null,
    transportType: 'PLAINTEXT',
    proxyReq: {
        address: null,
        port: null
    }
}

queryDescriptorBtnContainer.onclick = function () {
    if (reqInstance.address === '' || reqInstance.port === '' || reqInstance.fullService === '') {
        toastComponent.createToast('warning', 'Please fill in all fields.')
        return
    }
    resultContainer.innerHTML = ''
    dataTableContainer.innerHTML = ''
    methodSelectContainer.innerHTML = ''
    queryDescriptor()
}

addressInputContainer.addEventListener('change', function (event) {
    reqInstance.address = event.target.value
})
protInputContainer.addEventListener('change', function (event) {
    reqInstance.port = event.target.value
})
fullServiceInputContainer.addEventListener('change', function (event) {
    reqInstance.fullService = event.target.value
})
proxyAddressInputContainer.addEventListener('change', function (event) {
    reqInstance.proxyReq.address = event.target.value
})
proxyPortInputContainer.addEventListener('change', function (event) {
    reqInstance.proxyReq.port = event.target.value
})

commitBtnContainer.onclick = function () {
    grpcCall()
}

function onDataInput(item, parentItem) {
    return function (event) {
        event.stopPropagation()
        item.value = event.target.value
        if (parentItem) {
            if (parentItem.value == null) parentItem.value = {}
            parentItem.value[item.name] = item.value
        }
    };
}

function processTableDom(index, item, parentItem) {
    const tr = document.createElement('tr')

    // Add method column
    const indexNum = document.createElement('td')
    indexNum.textContent = index + 1
    tr.appendChild(indexNum)

    // Add field column
    const fieldTd = document.createElement('td')
    fieldTd.textContent = item.name
    tr.appendChild(fieldTd)

    // Add type column
    const typeTd = document.createElement('td')
    typeTd.textContent = item.type
    tr.appendChild(typeTd)

    // Add value column with input
    const valueTd = document.createElement('td')

    switch (item.type) {
        case 'INT32':
        case 'INT64':
        case 'DOUBLE':
        case 'FLOAT': {
            const label = document.createElement('label')
            label.classList.add('input', 'input-bordered', 'input-secondary', 'w-full', 'max-w-xs')
            const input = document.createElement('input')

            input.type = 'number'

            input.addEventListener('input', onDataInput(item, parentItem))
            label.appendChild(input)
            valueTd.appendChild(label)
            break
        }
        case 'STRING': {
            const label = document.createElement('label')
            label.classList.add('input', 'input-bordered', 'input-secondary', 'w-full', 'max-w-xs')
            const input = document.createElement('input')

            input.type = 'text'

            input.addEventListener('input', onDataInput(item, parentItem))
            label.appendChild(input)
            valueTd.appendChild(label)
            break
        }
        case 'BOOL': {
            const label = document.createElement('label')
            label.classList.add('form-control', 'w-full', 'max-w-xs')
            const select = document.createElement('select')
            select.classList.add('select', 'select-bordered', 'select-sm')
            const trueOption = document.createElement('option');
            const falseOption = document.createElement('option');
            trueOption.value = 'true'
            trueOption.textContent = 'true'
            falseOption.value = 'false'
            falseOption.textContent = 'false'
            select.addEventListener('change', function (event) {
                event.stopPropagation()
                item.value = event.target.value
                if (parentItem) parentItem[item.name] = event.target.value
            })
            select.value = 'true'
            item.value = 'true'
            select.appendChild(trueOption)
            select.appendChild(falseOption)
            label.appendChild(select)
            valueTd.appendChild(label)
            break
        }
        case 'MESSAGE': {
            item.childFields.forEach((item2, index) => {
                valueTd.appendChild(processTableDom(index, item2, item))
            })
        }
    }

    tr.appendChild(valueTd)
    return tr;
}

methodSelectContainer.addEventListener('change', function (event) {
    event.stopPropagation()
    dataTableContainer.innerHTML = ''
    const value = this.value
    reqInstance.method = value
    const descriptorData_v = descriptorData.find(e => e.method === value);
    reqInstance.message = descriptorData_v.message
    reqInstance.fieldList = descriptorData_v.fieldList
    if (reqInstance.fieldList == null) {
        return
    }
    reqInstance.fieldList.forEach((item, index) => {
        dataTableContainer.appendChild(processTableDom(index, item))
    })
})

transportTypeRadioContainers.forEach(radio => {
    radio.addEventListener('change', function() {
        reqInstance.transportType = this.value
        if (this.value === 'PLAINTEXT') {
            fileInputBoxContainer.classList.add('hidden');
        } else {
            fileInputBoxContainer.classList.remove('hidden');
        }
    });
});

/**
 * Get information about the method and its field descriptions
 * @param address
 * @param port
 * @param fullService
 * @param proxyAddress
 * @param proxyPort
 */

function queryDescriptor() {
    loadingContainer.classList.remove('hidden')
    reqInstance.method = null
    reqInstance.message = null
    reqInstance.fieldList = null
    fetch(QUERY_DESCRIPTOR_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(reqInstance)
    }).then(async response => {
        if (response.ok) {
            return response.json();
        } else {
            throw new Error(await response.text());
        }
    }).then(data => {
        flushMethodSelect(data)
        loadingContainer.classList.add('hidden')
        toastComponent.createToast('success', 'success.')
    }).catch(error => {
        loadingContainer.classList.add('hidden')
        toastComponent.createToast('error', error)
    })
}

function grpcCall() {
    loadingContainer.classList.remove('hidden')
    fetch(GRPC_CALL_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(reqInstance)
    }).then(async response => {
        const body = response.text();
        if (response.ok) {
            return body;
        } else {
            throw new Error(await body);
        }
    }).then(data => {
        resultContainer.innerHTML = data
        toastComponent.createToast('success', 'success.')
        loadingContainer.classList.add('hidden')
    }).catch(error => {
        resultContainer.innerHTML = error
        toastComponent.createToast('error', error)
        loadingContainer.classList.add('hidden')
    })
}

/**
 * Refresh method dropdown box based on requested data
 * @param data
 */

function flushMethodSelect(data) {
    methodSelectContainer.innerHTML = ''
    descriptorData.length = 0
    data.forEach(item => {
        const option = document.createElement('option')
        option.value = item.method
        option.textContent = item.method
        methodSelectContainer.appendChild(option)
    })
    if (data?.length > 0) {
        methodSelectContainer.value = data[0].method
    }
    descriptorData.push(...data)
    methodSelectContainer.dispatchEvent(new Event('change'));
}


