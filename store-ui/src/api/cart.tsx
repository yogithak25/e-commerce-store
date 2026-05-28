import axiosClient, { cartUrl } from "./config"

const addToCart = async (item: any) => {
    try {

        const payload = {
            customerId: "john@example.com",
            items: [
                {
                    productId: item?.productId,
                    sku: item?.sku,
                    title: item?.title,
                    quantity: item?.quantity,
                    price: item?.price,
                    currency: item?.currency
                }
            ]
        }

        console.log("Adding to cart:", payload)

        const response = await axiosClient.post(
            cartUrl + 'cart',
            payload
        )

        return response.data

    } catch (err: any) {
        console.log("ADD CART ERROR", err)
    }
}

export const getCart = async () => {

    try {

        const response = await axiosClient.get(
            cartUrl + 'cart/john@example.com'
        )

        console.log("RAW CART RESPONSE:", response.data)

        // CASE 1 -> API returns array
        if (Array.isArray(response.data)) {

            if (response.data.length > 0) {
                return response.data[0]
            }

            return { items: [] }
        }

        // CASE 2 -> API returns object
        if (response.data?.items) {
            return response.data
        }

        return { items: [] }

    } catch (err: any) {

        console.log("GET CART ERROR", err)

        return { items: [] }
    }
}

export default addToCart
