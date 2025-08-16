import React, { useContext, useState } from "react";
import "./PlaceOrder.css";
import { StoreContext } from "../../context/StoreContext";
import { calculateCartTotals } from "../../util/cartUtils";
import axios from "axios";
import { toast } from "react-toastify";
import { RAZORPAY_KEY } from "../../util/constant";
import { useNavigate } from "react-router-dom";

const PlaceOrder = () => {
  const { foodList, quantities, setQuantities, token } = useContext(StoreContext);
  const navigate = useNavigate();

  const [data, setData] = useState({
    firstName: "",
    lastName: "",
    email: "",
    phoneNumber: "",
    address: "",
    state: "",
    city: "",
    zip: "",
  });

  const onChangeHandler = (event) => {
    const { name, value } = event.target;
    setData((d) => ({ ...d, [name]: value }));
  };

  const cartItems = foodList.filter((food) => quantities[food.id] > 0);
  const { subtotal, shipping, tax, total } = calculateCartTotals(cartItems, quantities);

  const onSubmitHandler = async (event) => {
    event.preventDefault();

    if (!token) {
      toast.error("Please login to continue checkout.");
      navigate("/login");
      return;
    }
    if (cartItems.length === 0) {
      toast.error("Your cart is empty.");
      return;
    }

    const orderData = {
      userAddress: `${data.firstName} ${data.lastName}, ${data.address}, ${data.city}, ${data.state}, ${data.zip}`,
      phoneNumber: data.phoneNumber,
      email: data.email,
      orderItems: cartItems.map((item) => ({
        foodId: item.id,
        quantity: quantities[item.id],
        price: item.price * quantities[item.id],
        category: item.category,
        imageUrl: item.imageUrl,
        description: item.description,
        name: item.name,
      })),
      amount: Number(total.toFixed(2)), // backend will create Razorpay order using this
      orderStatus: "Preparing",
    };

    try {
      // Helpful once while debugging:
      // console.log("Bearer token?", token.slice(0, 15) + "...");

      const response = await axios.post(
        "http://localhost:8080/api/v1/orders/create",
        orderData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );

      const o = response.data;
      if (
        response.status === 201 &&
        o?.razorpayOrderId &&
        (Number.isInteger(o?.amountInPaise) || typeof o?.amount === "number")
      ) {
        initiateRazorpayPayment(o);
      } else {
        console.error("Create order missing fields:", response.data);
        toast.error("Unable to place order. Missing Razorpay order details.");
      }
    } catch (error) {
      console.error("Create order failed:", {
        status: error?.response?.status,
        data: error?.response?.data,
        headers: error?.response?.headers,
      });
      toast.error(error?.response?.data?.message || "Unable to place order, please try again.");
    }
  };

  const initiateRazorpayPayment = (order) => {
    if (!window.Razorpay) {
      toast.error("Payment not ready. Check if checkout.js loaded / disable adblock.");
      console.error("window.Razorpay is undefined");
      return;
    }

    // Prefer server-provided paise; fallback to computed amount
    const amountPaise = Number.isInteger(order.amountInPaise)
      ? order.amountInPaise
      : Math.round((order.amount ?? 0) * 100);

    if (!String(order.razorpayOrderId).startsWith("order_")) {
      console.error("Invalid razorpayOrderId:", order.razorpayOrderId);
      toast.error("Invalid payment order id.");
      return;
    }
    if (!Number.isInteger(amountPaise) || amountPaise <= 0) {
      console.error("Invalid amount (paise):", amountPaise);
      toast.error("Invalid payment amount.");
      return;
    }

    const options = {
      key: RAZORPAY_KEY, // use rzp_test_... key in dev
      order_id: order.razorpayOrderId,
      amount: amountPaise, // integer paise; must match Razorpay Order
      currency: order.currency || "INR",
      name: "Food Land",
      description: "Food Order Payment",
      prefill: {
        name: `${data.firstName} ${data.lastName}`,
        email: data.email,
        contact: data.phoneNumber,
      },
      theme: { color: "#3399cc" },
      handler: async function (rzpRes) {
        await verifyPayment(rzpRes);
      },
      modal: {
        ondismiss: async function () {
          toast.error("Payment was cancelled.");
          if (order.id) {
            try {
              await deleteOrder(order.id);
            } catch {
              /* ignore */
            }
          }
        },
      },
    };

    try {
      const rzp = new window.Razorpay(options);

      // Surface failure details instead of failing silently
      rzp.on("payment.failed", function (resp) {
        console.error("Razorpay payment.failed:", resp?.error);
        toast.error(resp?.error?.description || "Payment failed. Please try again.");
      });

      rzp.open();
    } catch (e) {
      console.error("Razorpay open() error:", e);
      toast.error("Couldn't open payment window. See console for details.");
    }
  };

  const verifyPayment = async (razorpayResponse) => {
    const paymentData = {
      razorpay_payment_id: razorpayResponse.razorpay_payment_id,
      razorpay_order_id: razorpayResponse.razorpay_order_id,
      razorpay_signature: razorpayResponse.razorpay_signature,
    };

    try {
      const response = await axios.post(
        "http://localhost:8080/api/v1/orders/verify",
        paymentData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );
      if (response.status === 200) {
        toast.success("Payment successful.");
        await clearCart();
        navigate("/myorders");
      } else {
        toast.error("Payment verification failed.");
        navigate("/");
      }
    } catch {
      toast.error("Payment verification failed.");
    }
  };

  const deleteOrder = async (orderId) => {
    try {
      await axios.delete(`http://localhost:8080/api/v1/orders/${orderId}`, {
        headers: { Authorization: `Bearer ${token}` },
      });
    } catch {
      toast.error("Something went wrong. Contact support.");
    }
  };

  const clearCart = async () => {
    try {
      await axios.delete("http://localhost:8080/api/v1/cart", {
        headers: { Authorization: `Bearer ${token}` },
      });
      setQuantities({});
    } catch {
      toast.error("Error while clearing the cart.");
    }
  };

  return (
    <div className="container mt-4">
      <main>
        <div className="row g-5">
          {/* Cart Section */}
          <div className="col-md-5 col-lg-4 order-md-last">
            <h4 className="d-flex justify-content-between align-items-center mb-3">
              <span className="text-primary">Your cart</span>
              <span className="badge bg-primary rounded-pill">{cartItems.length}</span>
            </h4>
            <ul className="list-group mb-3">
              {cartItems.map((item) => (
                <li key={item.id} className="list-group-item d-flex justify-content-between lh-sm">
                  <div>
                    <h6 className="my-0">{item.name}</h6>
                    <small className="text-body-secondary">Qty: {quantities[item.id]}</small>
                  </div>
                  <span className="text-body-secondary">₹{item.price * quantities[item.id]}</span>
                </li>
              ))}
              <li className="list-group-item d-flex justify-content-between">
                <div>
                  <span>Shipping</span>
                </div>
                <span className="text-body-secondary">
                  ₹{subtotal === 0 ? 0.0 : shipping.toFixed(2)}
                </span>
              </li>
              <li className="list-group-item d-flex justify-content-between">
                <div>
                  <span>Tax (10%)</span>
                </div>
                <span className="text-body-secondary">₹{tax.toFixed(2)}</span>
              </li>
              <li className="list-group-item d-flex justify-content-between">
                <span>Total (INR)</span>
                <strong>₹{total.toFixed(2)}</strong>
              </li>
            </ul>
          </div>

          {/* Billing Section */}
          <div className="col-md-7 col-lg-8">
            <h4 className="mb-3">Billing address</h4>
            <form className="needs-validation" onSubmit={onSubmitHandler}>
              <div className="row g-3">
                <div className="col-sm-6">
                  <label className="form-label">First name</label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    name="firstName"
                    onChange={onChangeHandler}
                    value={data.firstName}
                  />
                </div>
                <div className="col-sm-6">
                  <label className="form-label">Last name</label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    name="lastName"
                    onChange={onChangeHandler}
                    value={data.lastName}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Email</label>
                  <input
                    type="email"
                    className="form-control"
                    placeholder="Email"
                    required
                    name="email"
                    onChange={onChangeHandler}
                    value={data.email}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Phone Number</label>
                  <input
                    type="number"
                    className="form-control"
                    placeholder="9999999999"
                    required
                    name="phoneNumber"
                    onChange={onChangeHandler}
                    value={data.phoneNumber}
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Address</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="1234 Main St"
                    required
                    name="address"
                    onChange={onChangeHandler}
                    value={data.address}
                  />
                </div>
                <div className="col-md-5">
                  <label className="form-label">State</label>
                  <select
                    className="form-select"
                    required
                    name="state"
                    onChange={onChangeHandler}
                    value={data.state}
                  >
                    <option value="">Choose...</option>
                    <option>Karnataka</option>
                  </select>
                </div>
                <div className="col-md-4">
                  <label className="form-label">City</label>
                  <select
                    className="form-select"
                    required
                    name="city"
                    onChange={onChangeHandler}
                    value={data.city}
                  >
                    <option value="">Choose...</option>
                    <option>Bangalore</option>
                  </select>
                </div>
                <div className="col-md-3">
                  <label className="form-label">Zip</label>
                  <input
                    type="number"
                    className="form-control"
                    placeholder="12345"
                    required
                    name="zip"
                    onChange={onChangeHandler}
                    value={data.zip}
                  />
                </div>
              </div>
              <hr className="my-4" />
              <button className="w-100 btn btn-primary btn-lg" type="submit">
                Continue to checkout
              </button>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
};

// ✅ Correct export
export default PlaceOrder;