import ToastComponent from './toast.js'

const BASE_URL = window.location.href

const QUERY_DESCRIPTOR_URL = BASE_URL + 'api/grpc/query'

const GRPC_CALL_URL = BASE_URL + 'api/grpc/call'

const descriptorData = []
const toastComponent = new ToastComponent()
let currentDescriptorValue = null

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

queryDescriptorBtnContainer.onclick = function () {
    let address = addressInputContainer.value
    let port = protInputContainer.value
    let fullService = fullServiceInputContainer.value

    if (address === '' || port === '' || fullService === '') {
        toastComponent.createToast('warning', 'Please fill in all fields.')
        return
    }
    resultContainer.innerHTML = ''
    dataTableContainer.innerHTML = ''
    methodSelectContainer.innerHTML = ''
    queryDescriptor(address, port, fullService, proxyAddressInputContainer.value,proxyPortInputContainer.value)
}


commitBtnContainer.onclick = function () {
    grpcCall(addressInputContainer.value, protInputContainer.value,
        fullServiceInputContainer.value, methodSelectContainer.value,
        currentDescriptorValue.message, currentDescriptorValue.fieldList,
        proxyAddressInputContainer.value, proxyPortInputContainer.value)
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
    currentDescriptorValue = descriptorData.find(e => e.method === value)
    const fieldList = currentDescriptorValue?.fieldList
    if (fieldList == null) {
        return
    }
    fieldList.forEach((item, index) => {
        dataTableContainer.appendChild(processTableDom(index, item))
    })
})

/**
 * Get information about the method and its field descriptions
 * @param address
 * @param port
 * @param fullService
 * @param proxyAddress
 * @param proxyPort
 */

function queryDescriptor(address, port, fullService, proxyAddress, proxyPort) {
    loadingContainer.classList.remove('hidden')
    fetch(QUERY_DESCRIPTOR_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            address: address,
            port: port,
            fullService: fullService,
            transportType: 'PLAINTEXT',
            proxyReq: {
                address: proxyAddress,
                port: proxyPort
            }
        })
    }).then(async response => {
        if (response.ok) {
            return response.json();
        } else {
            // 处理错误响应，例如4xx或5xx状态码
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

function grpcCall(address, port, fullService, method, message, fieldList, proxyAddress, proxyPort) {
    loadingContainer.classList.remove('hidden')
    fetch(GRPC_CALL_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            address: address,
            port: port,
            fullService: fullService,
            method: method,
            message: message,
            fieldList: fieldList,
            transportType: 'PLAINTEXT',
            proxyReq: {
                address: proxyAddress,
                port: proxyPort
            }
        })
    }).then(async response => {
        const body = response.text();
        if (response.ok) {
            return body;
        } else {
            // 处理错误响应，例如4xx或5xx状态码
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


