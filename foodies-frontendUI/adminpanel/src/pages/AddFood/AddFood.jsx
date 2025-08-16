import React, { useState } from 'react';
import { assets } from '../../assets/assets';
import axios from 'axios';

const AddFood = () => {
  const [image, setImage] = useState(null);
  const [data, setData] = useState({
    name: '',
    description: '',
    price: '',
    category: 'Burger',
  });

  const onChangeHandler = (event) => {
    const { name, value } = event.target;
    setData((prev) => ({ ...prev, [name]: value }));
  };

  const onSubmitHandler = async (event) => {
    event.preventDefault();

    if (!image) {
      alert('Please select an image');
      return;
    }

    const payload = {
      ...data,
      // ensure price is numeric if backend expects a number
      price: data.price === '' ? '' : Number(data.price),
    };

    const formData = new FormData();
    formData.append('food', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    formData.append('file', image);

/*API call to backend */    
    try {
      const response = await axios.post('http://localhost:8080/api/v1/foods', formData);
      if (response.status === 200) {
        alert('Food added successfully');
        setData({ name: '', description: '', category: 'Burger', price: '' });
        setImage(null);
      }
    } catch (error) {
      console.error('Error', error);
      alert('Error adding food');
    }
  };

  return (
    <div className="mx-2 mt-2">
      <div className="row">
        <div className="card col-md-4">
          <div className="card-body">
            <h2 className="mb-4">Add Food</h2>
            <form onSubmit={onSubmitHandler}>
              <div className="mb-3">
                <label htmlFor="image" className="form-label" style={{ cursor: 'pointer' }}>
                  <img
                    src={image ? URL.createObjectURL(image) : assets.upload}
                    alt="Upload preview"
                    width={85}
                  />
                </label>
                <input
                  type="file"
                  className="form-control"
                  id="image"
                  hidden
                  accept="image/*"
                  onChange={(e) => setImage(e.target.files?.[0] ?? null)}
                />
              </div>

              <div className="mb-3">
                <label htmlFor="name" className="form-label">Name</label>
                <input
                  type="text"
                  placeholder='Burger'
                  className="form-control"
                  id="name"
                  required
                  name="name"
                  onChange={onChangeHandler}
                  value={data.name}
                />
              </div>

              <div className="mb-3">
                <label htmlFor="description" className="form-label">Description</label>
                <textarea
                  className="form-control"
                  placeholder='Write content here..'
                  id="description"
                  rows="5"
                  required
                  name="description"
                  onChange={onChangeHandler}
                  value={data.description}
                />
              </div>

              <div className="mb-3">
                <label htmlFor="category" className="form-label">Category</label>
                <select
                  name="category"
                  id="category"
                  className="form-control"
                  onChange={onChangeHandler}
                  value={data.category}
                >
                  <option value="Biryani">Biryani</option>
                  <option value="Cake">Cake</option>
                  <option value="Pizza">Pizza</option>
                  <option value="Salad">Salad</option>
                  <option value="Burger">Burger</option>
                  <option value="Ice-Cream">Ice-Cream</option>
                </select>
              </div>

              <div className="mb-3">
                <label htmlFor="price" className="form-label">Price</label>
                <input
                  type="number"
                  placeholder='&#8377;200'
                  name="price"
                  id="price"
                  className="form-control"
                  min="0"
                  step="0.01"
                  onChange={onChangeHandler}
                  value={data.price}
                />
              </div>

              <button type="submit" className="btn btn-primary">Save</button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AddFood;