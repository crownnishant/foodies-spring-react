import React, { useState } from 'react';
import { Route, Routes } from 'react-router-dom';
import AddFood from './pages/AddFood/AddFood.jsx';     // Update path as per your file location
import ListFoods from './pages/ListFoods/ListFood.jsx';
import Orders from './pages/Orders/Orders.jsx';
import Sidebar from './components/Sidebar/Sidebar.jsx';
import Menubar from './components/Menubar/Menubar.jsx';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

const App = () => {
  const [sidebarVisible, setSidebarVisible]= useState(true);

  const toggleSidebar=()=> {
    setSidebarVisible(!sidebarVisible);
  }

  return (
    <div className="d-flex" id="wrapper">
      <ToastContainer />
      {/* Sidebar */}
      <Sidebar  sidebarVisible={sidebarVisible}/>

      <div id="page-content-wrapper">

        <Menubar toggleSidebar={toggleSidebar}/>
       
        <div className="container-fluid">
          <Routes>
            <Route path='add' element={<AddFood />} />
            <Route path='list' element={<ListFoods />} />
            <Route path='orders' element={<Orders />} />
            <Route path='/' element={<ListFoods />} />
          </Routes>
        </div>
      </div>
    </div>
  );
};

export default App;