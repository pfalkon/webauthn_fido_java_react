/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import InputForm from "../../forms/InputForm";

class UserRegistration extends React.Component {

    constructor(props) {
        super(props);
        this.onRegisterDeviceClick = this.onRegisterDeviceClick.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
    }

    handleInputChange(e){
        e.preventDefault();
        this.props.handleUserInput(e.target.name, e.target.value);

    }

    onRegisterDeviceClick(e){
        e.preventDefault();
        this.props.registerUser();
    }


    render(){
        const {firstName, lastName, email} = this.props;
        return (
            <form className="measure" key="form_user_registration">
                <fieldset className="ba b--transparent ph0 mh0">
                    <div className="mt3" key="user_registration_email">
                         <label className="db fw6 lh-copy f6" htmlFor="email-address">Email</label>
                        <InputForm name="user_email" value={email} type="email" onChangeText={this.handleInputChange}/>
                    </div>
                    <div className="mt3" key="eee">
                        <label className="db fw6 lh-copy f6" htmlFor="Last Name">Last Name</label>
                        <InputForm name="lastName" value={lastName} type="text" onChangeText={this.handleInputChange}/>
                    </div>
                    <div className="mt3" key="eeeaa">
                        <label className="db fw6 lh-copy f6" htmlFor="First Name">First Name</label>
                        <InputForm name="firstName" value={firstName} type="text" onChangeText={this.handleInputChange}/>
                    </div>
                    <div className="mt3">
                        <button name="register" value="Register" onClick={this.onRegisterDeviceClick} className="b ph3 pv2 input-reset ba  grow pointer f6 dib">Register</button>
                    </div>
                </fieldset>
            </form>
        )
    }
}

export default UserRegistration;